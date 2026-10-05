package co.fcv.citas.application.appointment.port.out;

import co.fcv.citas.domain.appointment.AppointmentStatus;
import co.fcv.citas.domain.appointment.ChangeSource;

/** Puerto de salida para la auditoria de transiciones que exige RF-19. */
public interface StatusHistoryPort {

  /**
   * Registra la transicion.
   *
   * @param actorUserId responsable, o {@code null} cuando el cambio es del sistema
   */
  void record(long appointmentId, AppointmentStatus status, Long actorUserId,
              ChangeSource source, String reason);
}
