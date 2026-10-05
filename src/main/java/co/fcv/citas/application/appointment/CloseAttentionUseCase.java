package co.fcv.citas.application.appointment;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.fcv.citas.application.appointment.port.out.AppointmentRepositoryPort;
import co.fcv.citas.application.appointment.port.out.StatusHistoryPort;
import co.fcv.citas.domain.appointment.Appointment;
import co.fcv.citas.domain.appointment.AppointmentStatus;
import co.fcv.citas.domain.appointment.ChangeSource;
import co.fcv.citas.domain.shared.DomainRuleViolation;

/**
 * HU-026 — Cerrar la atencion como atendida o no asistio.
 *
 * <p>No libera la franja, y eso es la regla y no un olvido: la atencion ocurrio y el horario quedo
 * consumido. Liberarla permitiria reservar sobre una atencion ya prestada.
 */
@Service
public class CloseAttentionUseCase {

  private final AppointmentRepositoryPort appointments;
  private final StatusHistoryPort history;

  public CloseAttentionUseCase(AppointmentRepositoryPort appointments, StatusHistoryPort history) {
    this.appointments = appointments;
    this.history = history;
  }

  /**
   * @param professionalId el profesional del token, no un parametro de la peticion
   * @param actorUserId el usuario que cierra, para la auditoria
   */
  @Transactional
  public Appointment close(long appointmentId, AppointmentStatus outcome, long professionalId,
                           long actorUserId, String notes) {
    var appointment = appointments.findById(appointmentId)
        .orElseThrow(() -> DomainRuleViolation.conflictingState("la cita no existe"));

    appointment.closeAttention(outcome, professionalId, LocalDateTime.now());

    appointments.updateStatus(appointment, null);
    history.record(appointmentId, outcome, actorUserId, ChangeSource.USER, notes);
    return appointment;
  }

  /** Cierto si la cita existe y es de ese profesional: distingue el 404 del conflicto de estado. */
  public boolean isAttendedBy(long appointmentId, long professionalId) {
    return appointments.findById(appointmentId)
        .filter(found -> found.isAttendedBy(professionalId))
        .isPresent();
  }
}
