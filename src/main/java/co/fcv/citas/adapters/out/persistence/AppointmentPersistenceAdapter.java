package co.fcv.citas.adapters.out.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import co.fcv.citas.application.appointment.port.out.AppointmentRepositoryPort;
import co.fcv.citas.domain.appointment.Appointment;
import co.fcv.citas.domain.appointment.SlotPlan;

/**
 * Adaptador de salida: implementa el puerto de citas con Spring Data JPA.
 *
 * <p>Su unica responsabilidad es traducir. Hacia dentro construye el agregado del dominio; hacia
 * fuera escribe la entidad. No toma ninguna decision de negocio: cuando recibe un {@link Appointment}
 * ya decidido, se limita a reflejar su estado.
 */
@Component
class AppointmentPersistenceAdapter implements AppointmentRepositoryPort {

  private final AppointmentJpaRepository repository;
  private final AppointmentStatusCatalog statuses;

  AppointmentPersistenceAdapter(AppointmentJpaRepository repository, AppointmentStatusCatalog statuses) {
    this.repository = repository;
    this.statuses = statuses;
  }

  @Override
  public long create(NewAppointment appointment) {
    var entity = new AppointmentEntity(
        appointment.patientUserId(), appointment.professionalId(),
        (short) appointment.locationId(), (short) appointment.specialtyId(),
        statuses.idOf(appointment.status()), appointment.startAt(), appointment.endAt(),
        appointment.createdByUserId());
    // saveAndFlush y no save: quien llama necesita el identificador de inmediato para reclamar las
    // franjas, y con save() el INSERT podria no haberse ejecutado todavia.
    return repository.saveAndFlush(entity).getId();
  }

  @Override
  public Optional<Appointment> findById(long id) {
    return repository.findById(id).map(this::toDomain);
  }

  @Override
  public void updateStatus(Appointment appointment, Long decidedByUserId) {
    var entity = repository.findById(appointment.id())
        .orElseThrow(() -> new IllegalStateException("la cita " + appointment.id() + " ya no existe"));
    boolean approved = appointment.status() == co.fcv.citas.domain.appointment.AppointmentStatus.APPROVED;
    if (decidedByUserId != null) {
      entity.applyDecision(statuses.idOf(appointment.status()), appointment.reason(),
          approved ? decidedByUserId : null, approved ? LocalDateTime.now() : null);
    } else {
      entity.applyStatus(statuses.idOf(appointment.status()));
    }
    repository.saveAndFlush(entity);
  }

  @Override
  public void updateSchedule(Appointment appointment) {
    var entity = repository.findById(appointment.id())
        .orElseThrow(() -> new IllegalStateException("la cita " + appointment.id() + " ya no existe"));
    entity.reschedule(appointment.slots().start(), appointment.slots().end(),
        (short) appointment.locationId());
    repository.saveAndFlush(entity);
  }

  /**
   * Rehidrata y no valida: {@code SlotPlan.of} comprueba la frontera de franja y la duracion, que son
   * invariantes de <b>creacion</b>. Aplicarlas al leer haria ilegible cualquier cita guardada que no
   * las cumpliera, y un dato historico no es un error de la peticion actual.
   */
  private Appointment toDomain(AppointmentEntity entity) {
    return new Appointment(entity.getId(), entity.getPatientUserId(), entity.getProfessionalId(),
        entity.getLocationId(), entity.getSpecialtyId(),
        SlotPlan.rehydrate(entity.getScheduledStartAt(), entity.getScheduledEndAt()),
        statuses.statusOf(entity.getStatusId()), entity.getReason());
  }
}
