package co.fcv.citas.domain.appointment;

/**
 * Los seis estados de una cita del PRD, con las reglas de transicion que antes vivian repartidas en
 * las condiciones SQL de cada controlador.
 *
 * <p>Tenerlas aqui es el punto de la arquitectura: la pregunta «¿se puede cancelar esta cita?» tiene
 * una sola respuesta y se puede comprobar sin base de datos. Antes estaba escrita tres veces, en tres
 * clausulas {@code WHERE} distintas, y nada garantizaba que las tres dijeran lo mismo.
 */
public enum AppointmentStatus {

  /** Cita especializada pendiente de decision administrativa (RN-03). */
  REQUESTED,
  /** Cita vigente: general auto-aprobada (RN-02) o especializada aprobada. */
  APPROVED,
  /** Rechazada por el ADMIN. Exige motivo (RN-04) y libera la franja. */
  REJECTED,
  /** Cancelada por su titular. Libera la franja. */
  CANCELLED,
  /** Atencion prestada. No libera la franja: el horario se consumio. */
  COMPLETED,
  /** El paciente no asistio. Tampoco libera la franja. */
  NO_SHOW;

  /**
   * Un estado terminal no admite ninguna transicion mas.
   *
   * <p>{@code COMPLETED} y {@code NO_SHOW} describen una atencion que ya ocurrio, y cambiarla
   * reescribiria un hecho. {@code REJECTED} y {@code CANCELLED} ya liberaron su franja, de modo que
   * revivirlas dejaria una cita sin horario reservado.
   */
  public boolean isTerminal() {
    return this == REJECTED || this == CANCELLED || this == COMPLETED || this == NO_SHOW;
  }

  /** Mantiene ocupada la franja del profesional. */
  public boolean holdsSlots() {
    return this == REQUESTED || this == APPROVED || this == COMPLETED || this == NO_SHOW;
  }

  /** El titular puede cancelar lo que aun no ha ocurrido ni se ha resuelto. */
  public boolean isCancellableByPatient() {
    return this == REQUESTED || this == APPROVED;
  }

  /** Solo una cita vigente puede reprogramarse: una solicitud aun no tiene horario que mover. */
  public boolean isReschedulable() {
    return this == APPROVED;
  }

  /** El ADMIN solo decide sobre lo que esta pendiente de decision. */
  public boolean awaitsAdminDecision() {
    return this == REQUESTED;
  }

  /** El profesional cierra la atencion de una cita vigente. */
  public boolean isAttentionCloseable() {
    return this == APPROVED;
  }

  /** Resultados admitidos al cerrar la atencion (RF-17). */
  public boolean isAttentionOutcome() {
    return this == COMPLETED || this == NO_SHOW;
  }

  /**
   * Convierte el codigo almacenado. Falla en lugar de devolver null: un estado desconocido en la
   * base es un defecto, y descubrirlo aqui es mejor que arrastrar un {@code null} hasta una regla.
   */
  public static AppointmentStatus of(String code) {
    if (code == null) throw new IllegalArgumentException("código de estado ausente");
    try {
      return valueOf(code.trim().toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("estado de cita desconocido: " + code);
    }
  }
}
