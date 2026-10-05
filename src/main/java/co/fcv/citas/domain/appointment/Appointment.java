package co.fcv.citas.domain.appointment;

import java.time.LocalDateTime;

import co.fcv.citas.domain.shared.DomainRuleViolation;

/**
 * Una cita, con las transiciones que admite y las que no.
 *
 * <p>Es el nucleo del dominio y no conoce Spring, JPA ni HTTP: se construye con datos planos y se
 * puede ejercer en una prueba unitaria sin levantar nada. Esa es la prueba de que
 * «dominio/aplicacion independientes de adaptadores» es cierto y no solo una carpeta con ese nombre.
 *
 * <p>Cada metodo que cambia el estado comprueba primero si la transicion es legitima y, si no,
 * lanza. Antes esas comprobaciones vivian como condiciones dentro del {@code WHERE} de un
 * {@code UPDATE}: funcionaban, pero no se podian leer juntas ni probar sin base de datos, y la misma
 * regla estaba escrita en varios sitios sin nada que garantizara que coincidian.
 */
public class Appointment {

  private final long id;
  private final long patientUserId;
  private final long professionalId;
  private final long locationId;
  private final long specialtyId;
  private SlotPlan slots;
  private AppointmentStatus status;
  private String reason;

  public Appointment(long id, long patientUserId, long professionalId, long locationId,
                     long specialtyId, SlotPlan slots, AppointmentStatus status, String reason) {
    this.id = id;
    this.patientUserId = patientUserId;
    this.professionalId = professionalId;
    this.locationId = locationId;
    this.specialtyId = specialtyId;
    this.slots = slots;
    this.status = status;
    this.reason = reason;
  }

  public long id() { return id; }
  public long patientUserId() { return patientUserId; }
  public long professionalId() { return professionalId; }
  public long locationId() { return locationId; }
  public long specialtyId() { return specialtyId; }
  public SlotPlan slots() { return slots; }
  public AppointmentStatus status() { return status; }
  public String reason() { return reason; }

  public boolean belongsTo(long userId) { return patientUserId == userId; }

  public boolean isAttendedBy(long professional) { return professionalId == professional; }

  // --- HU-023: cancelacion por el titular ---------------------------------------------------

  /**
   * CA-01 y CA-02. Tres condiciones, no una: la cita es suya, esta en un estado cancelable y aun no
   * ha empezado.
   */
  public void cancelBy(long userId, LocalDateTime now) {
    if (!belongsTo(userId)) {
      throw DomainRuleViolation.conflictingState("La cita no puede cancelarse");
    }
    if (!status.isCancellableByPatient()) {
      throw DomainRuleViolation.conflictingState("La cita no puede cancelarse");
    }
    if (slots.startsBefore(now)) {
      throw DomainRuleViolation.conflictingState("La cita no puede cancelarse");
    }
    status = AppointmentStatus.CANCELLED;
  }

  // --- HU-028: decision administrativa -------------------------------------------------------

  /**
   * CA-01 a CA-03. RN-04 hace obligatorio el motivo del rechazo, y aqui es una condicion del
   * dominio y no una validacion de formulario: una cita rechazada sin motivo no debe poder existir.
   */
  public void decide(AppointmentStatus decision, String decisionReason) {
    if (decision != AppointmentStatus.APPROVED && decision != AppointmentStatus.REJECTED) {
      throw DomainRuleViolation.invalidRequest("decisión inválida");
    }
    if (decision == AppointmentStatus.REJECTED && (decisionReason == null || decisionReason.isBlank())) {
      throw DomainRuleViolation.invalidRequest("motivo requerido");
    }
    if (!status.awaitsAdminDecision()) {
      throw DomainRuleViolation.conflictingState("la solicitud ya fue resuelta");
    }
    status = decision;
    reason = decisionReason;
  }

  // --- HU-026: cierre de atencion ------------------------------------------------------------

  /**
   * CA-01 a CA-03. Solo el profesional que atiende, solo una cita vigente y solo una vez comenzada.
   * La franja <b>no</b> se libera: la atencion ocurrio y el horario quedo consumido.
   */
  public void closeAttention(AppointmentStatus outcome, long professional, LocalDateTime now) {
    if (outcome == null || !outcome.isAttentionOutcome()) {
      throw DomainRuleViolation.invalidRequest("resultado debe ser COMPLETED o NO_SHOW");
    }
    if (!isAttendedBy(professional)) {
      throw DomainRuleViolation.conflictingState("la cita no es de este profesional");
    }
    if (!status.isAttentionCloseable()) {
      throw DomainRuleViolation.conflictingState(
          "la cita no es cerrable: debe estar APPROVED y haber comenzado");
    }
    if (!slots.startsBefore(now)) {
      throw DomainRuleViolation.conflictingState(
          "la cita no es cerrable: debe estar APPROVED y haber comenzado");
    }
    status = outcome;
  }

  // --- HU-024 y HU-030: reprogramacion -------------------------------------------------------

  /** CA-03 de HU-024: solo una cita vigente y futura puede reprogramarse. */
  public void assertReschedulable(long userId, LocalDateTime now) {
    if (!belongsTo(userId)) {
      throw DomainRuleViolation.conflictingState("la cita no es suya");
    }
    if (!status.isReschedulable()) {
      throw DomainRuleViolation.conflictingState("solo una cita aprobada puede reprogramarse");
    }
    if (slots.startsBefore(now)) {
      throw DomainRuleViolation.conflictingState("la cita ya pasó");
    }
  }

  /**
   * Mueve la cita a la franja aprobada. Devuelve el plan anterior, que es lo que el adaptador
   * necesita para liberar lo que deja de usarse; la exclusion del solapamiento la decide
   * {@link SlotPlan#overlaps}.
   */
  public SlotPlan moveTo(SlotPlan approved) {
    if (approved == null) throw DomainRuleViolation.invalidRequest("falta la nueva franja");
    SlotPlan previous = slots;
    slots = approved;
    return previous;
  }
}
