package co.fcv.citas.application.appointment.port.out;

import java.util.Optional;

import co.fcv.citas.domain.appointment.Appointment;
import co.fcv.citas.domain.appointment.AppointmentStatus;

/** Puerto de salida para el estado de las citas. Habla de {@link Appointment}, no de filas. */
public interface AppointmentRepositoryPort {

  /** Crea la cita y devuelve su identificador asignado. */
  long create(NewAppointment appointment);

  Optional<Appointment> findById(long id);

  /**
   * Persiste el estado y el motivo de una cita ya decidida. Recibe el agregado completo porque las
   * reglas ya se aplicaron en el dominio: el adaptador no vuelve a decidir, solo escribe.
   */
  void updateStatus(Appointment appointment, Long decidedByUserId);

  /** Persiste el nuevo horario tras aprobar una reprogramacion. */
  void updateSchedule(Appointment appointment);

  record NewAppointment(long patientUserId, long professionalId, long locationId, long specialtyId,
                        AppointmentStatus status, java.time.LocalDateTime startAt,
                        java.time.LocalDateTime endAt, long createdByUserId) {}
}
