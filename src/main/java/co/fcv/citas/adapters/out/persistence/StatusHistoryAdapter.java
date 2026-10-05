package co.fcv.citas.adapters.out.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import co.fcv.citas.application.appointment.port.out.StatusHistoryPort;
import co.fcv.citas.domain.appointment.AppointmentStatus;
import co.fcv.citas.domain.appointment.ChangeSource;

/**
 * Adaptador de salida de la auditoria de transiciones (RF-19).
 *
 * <p>El {@code INSERT ... SELECT} resuelve el identificador del estado en la misma sentencia, de modo
 * que no hay dos viajes a la base por transicion registrada.
 */
@Component
class StatusHistoryAdapter implements StatusHistoryPort {

  private final JdbcTemplate db;

  StatusHistoryAdapter(JdbcTemplate db) { this.db = db; }

  @Override
  public void record(long appointmentId, AppointmentStatus status, Long actorUserId,
                     ChangeSource source, String reason) {
    db.update("INSERT INTO appointment_status_history"
        + "(appointment_id,status_id,changed_by_user_id,change_source,reason)"
        + " SELECT ?,s.id,?,?,? FROM appointment_statuses s WHERE s.code=?",
        appointmentId, actorUserId, source.name(), reason, status.name());
  }
}
