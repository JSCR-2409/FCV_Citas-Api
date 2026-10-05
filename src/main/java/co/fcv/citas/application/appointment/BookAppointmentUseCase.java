package co.fcv.citas.application.appointment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.fcv.citas.application.appointment.port.out.AppointmentRepositoryPort;
import co.fcv.citas.application.appointment.port.out.SlotReservationPort;
import co.fcv.citas.application.appointment.port.out.SpecialtyCatalogPort;
import co.fcv.citas.application.appointment.port.out.StatusHistoryPort;
import co.fcv.citas.domain.appointment.AppointmentStatus;
import co.fcv.citas.domain.appointment.ChangeSource;
import co.fcv.citas.domain.appointment.SlotPlan;
import co.fcv.citas.domain.appointment.SpecialtyKind;
import co.fcv.citas.domain.shared.DomainRuleViolation;

/**
 * HU-020 y HU-021 — Reservar una cita.
 *
 * <p>Este metodo es el que antes era una sola linea de 1.400 caracteres dentro de un controlador,
 * con la consulta de la especialidad, las reglas de franja, el {@code INSERT} y la auditoria
 * mezclados. Aqui se lee la secuencia de decisiones y cada paso de infraestructura es una llamada a
 * un puerto.
 *
 * <p>Lleva {@code @Service} y {@code @Transactional}, que son de Spring. Es una concesion consciente:
 * la alternativa —un adaptador que abra la transaccion y delegue en una clase sin anotaciones— añade
 * una capa de indireccion sin cambiar quien depende de quien. Lo que la arquitectura exige es que el
 * <b>dominio</b> no dependa de adaptadores, y {@code co.fcv.citas.domain} no importa nada de Spring.
 */
@Service
public class BookAppointmentUseCase {

  private final SpecialtyCatalogPort specialties;
  private final SlotReservationPort slots;
  private final AppointmentRepositoryPort appointments;
  private final StatusHistoryPort history;

  public BookAppointmentUseCase(SpecialtyCatalogPort specialties, SlotReservationPort slots,
                                AppointmentRepositoryPort appointments, StatusHistoryPort history) {
    this.specialties = specialties;
    this.slots = slots;
    this.appointments = appointments;
    this.history = history;
  }

  /**
   * @param expectedKind la clase que el endpoint usado implica. Si la especialidad es de la otra, la
   *     peticion se rechaza: el endpoint general no puede crear una cita que exige aprobacion, ni al
   *     contrario.
   */
  @Transactional
  public Result book(Command command, SpecialtyKind expectedKind) {
    if (command == null || command.slotId() == null || command.specialtyId() == null) {
      throw DomainRuleViolation.invalidRequest("datos incompletos");
    }

    var specialty = specialties.findActive(command.specialtyId())
        .filter(found -> found.kind() == expectedKind)
        .orElseThrow(() -> DomainRuleViolation.invalidRequest("especialidad no elegible"));

    var open = slots.findOpenSlot(command.slotId(), command.specialtyId())
        .orElseThrow(() -> DomainRuleViolation.conflictingState("franja no disponible"));

    // RN-05: el plan dice cuantas franjas hacen falta; la comprobacion de que estan libres es
    // previa al INSERT para no crear una cita que luego habria que deshacer en el caso comun.
    var plan = SlotPlan.of(open.startAt(), specialty.durationMinutes());
    if (slots.countFreeSlots(open.professionalId(), open.locationId(), plan) != plan.slotsNeeded()) {
      throw DomainRuleViolation.conflictingState("franja no disponible");
    }

    AppointmentStatus initial = expectedKind.initialStatus();
    long id = appointments.create(new AppointmentRepositoryPort.NewAppointment(
        command.patientUserId(), open.professionalId(), open.locationId(), command.specialtyId(),
        initial, plan.start(), plan.end(), command.patientUserId()));

    // RN-01. La guarda esta dentro del propio claim, de modo que dos reservas simultaneas no pueden
    // compartir franja. Si no consigue todas las que necesita, se deshace la transaccion entera:
    // una cita con media franja seria peor que ninguna cita.
    if (slots.claim(id, open.professionalId(), open.locationId(), plan) != plan.slotsNeeded()) {
      throw DomainRuleViolation.conflictingState("franja no disponible");
    }

    // HU-031: el estado inicial tambien es una transicion. Sin esto la auditoria empezaria a contar
    // la vida de la cita por su segundo estado.
    history.record(id, initial, command.patientUserId(), ChangeSource.USER, null);

    return new Result(id, initial, plan, open.professionalId(), open.locationId(), command.specialtyId());
  }

  public record Command(Long slotId, Long specialtyId, long patientUserId) {}

  public record Result(long id, AppointmentStatus status, SlotPlan slots,
                       long professionalId, long locationId, long specialtyId) {}
}
