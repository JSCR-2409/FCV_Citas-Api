package co.fcv.citas.adapters.out.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import co.fcv.citas.application.appointment.port.out.PendingRescheduleClosurePort;

/** Adaptador de salida: cierra las reprogramaciones pendientes de una cita cancelada. */
@Component
class PendingRescheduleClosureAdapter implements PendingRescheduleClosurePort {

  private final JdbcTemplate db;

  PendingRescheduleClosureAdapter(JdbcTemplate db) { this.db = db; }

  @Override
  public void cancelPendingFor(long appointmentId) {
    db.update("UPDATE reschedule_requests"
        + " SET status_id=(SELECT id FROM reschedule_request_statuses WHERE code='CANCELLED')"
        + " WHERE appointment_id=?"
        + "   AND status_id=(SELECT id FROM reschedule_request_statuses WHERE code='PENDING')",
        appointmentId);
  }
}
