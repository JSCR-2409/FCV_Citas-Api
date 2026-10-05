package co.fcv.citas.domain.appointment;

/**
 * Quien origino un cambio de estado, segun RF-19.
 *
 * <p>Distingue el origen, no el endpoint: {@code USER} es quien actua sobre su propia atencion —el
 * titular de la cita o el profesional que la cierra—, {@code ADMIN} es una decision de operacion, y
 * {@code SYSTEM} queda para los efectos en cascada que nadie pidio explicitamente.
 *
 * <p>Los valores coinciden con los que admite {@code ck_status_history_source}.
 */
public enum ChangeSource {
  SYSTEM,
  USER,
  ADMIN
}
