package co.fcv.citas.appointment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * HU-031 / RF-19 — Registro de cada cambio de estado de una cita.
 *
 * <p>La tabla {@code appointment_status_history} existia en el esquema desde el principio pero
 * ninguna transicion escribia en ella, de modo que la auditoria estaba vacia aunque las citas
 * cambiaran de estado. Centralizar la escritura aqui evita que una transicion nueva se olvide de
 * registrarse: quien cambia {@code appointments.status_id} llama a este componente en la misma
 * transaccion.
 *
 * <p>La fuente distingue quien origino el cambio, no que endpoint se uso: USER es el titular de la
 * cita, ADMIN y PROFESSIONAL son decisiones de operacion, y SYSTEM queda para los efectos en
 * cascada que nadie pidio explicitamente.
 */
@Component
public class AppointmentStatusLog {

  /** Valores admitidos por ck_status_history_source. */
  public static final String SOURCE_SYSTEM = "SYSTEM";
  public static final String SOURCE_USER = "USER";
  public static final String SOURCE_ADMIN = "ADMIN";

  private final JdbcTemplate db;

  public AppointmentStatusLog(JdbcTemplate db) { this.db = db; }

  /**
   * Registra la transicion a {@code statusCode}.
   *
   * @param actorUserId usuario responsable, o {@code null} cuando el cambio es del sistema
   * @param reason motivo opcional; obligatorio por regla de negocio en el rechazo, no aqui
   */
  public void record(long appointmentId, String statusCode, Long actorUserId, String source, String reason) {
    db.update("INSERT INTO appointment_status_history"
        + "(appointment_id,status_id,changed_by_user_id,change_source,reason)"
        + " SELECT ?,s.id,?,?,? FROM appointment_statuses s WHERE s.code=?",
        appointmentId, actorUserId, source, reason, statusCode);
  }
}
