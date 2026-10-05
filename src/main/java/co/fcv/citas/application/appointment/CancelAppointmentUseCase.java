package co.fcv.citas.application.appointment;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.fcv.citas.application.appointment.port.out.AppointmentRepositoryPort;
import co.fcv.citas.application.appointment.port.out.PendingRescheduleClosurePort;
import co.fcv.citas.application.appointment.port.out.SlotReservationPort;
import co.fcv.citas.application.appointment.port.out.StatusHistoryPort;
import co.fcv.citas.domain.appointment.Appointment;
import co.fcv.citas.domain.appointment.ChangeSource;
import co.fcv.citas.domain.shared.DomainRuleViolation;

/**
 * HU-023 — Cancelar una cita.
 *
 * <p>Las tres condiciones —la cita es suya, esta en un estado cancelable y aun no ha empezado— ya no
 * son una clausula {@code WHERE}: las comprueba {@link Appointment#cancelBy}, que es donde se pueden
 * leer juntas y probar sin base de datos.
 *
 * <p>Perder la atomicidad del {@code UPDATE} con guarda no abre una carrera aqui: la transaccion
 * es {@code REPEATABLE READ} y el unico actor que puede cancelar es el titular, de modo que no hay
 * dos escritores compitiendo como si los hay al reservar una franja.
 */
@Service
public class CancelAppointmentUseCase {

  private final AppointmentRepositoryPort appointments;
  private final SlotReservationPort slots;
  private final StatusHistoryPort history;
  private final PendingRescheduleClosurePort pendingReschedules;

  public CancelAppointmentUseCase(AppointmentRepositoryPort appointments, SlotReservationPort slots,
                                  StatusHistoryPort history,
                                  PendingRescheduleClosurePort pendingReschedules) {
    this.appointments = appointments;
    this.slots = slots;
    this.history = history;
    this.pendingReschedules = pendingReschedules;
  }

  @Transactional
  public Appointment cancel(long appointmentId, long requestedByUserId) {
    var appointment = appointments.findById(appointmentId)
        // El mismo mensaje para «no existe» y «no se puede»: distinguirlos permitiria averiguar que
        // citas existen probando identificadores.
        .orElseThrow(() -> DomainRuleViolation.conflictingState("La cita no puede cancelarse"));

    appointment.cancelBy(requestedByUserId, LocalDateTime.now());

    appointments.updateStatus(appointment, null);
    slots.releaseAll(appointmentId);
    // Cancelar la cita cierra tambien cualquier reprogramacion pendiente: el ADMIN ya no tiene que
    // decidir sobre una cita que no existe.
    pendingReschedules.cancelPendingFor(appointmentId);
    history.record(appointmentId, appointment.status(), requestedByUserId, ChangeSource.USER, null);

    return appointment;
  }
}
