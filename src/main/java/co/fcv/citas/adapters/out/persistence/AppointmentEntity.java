package co.fcv.citas.adapters.out.persistence;

import java.time.Instant;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * La cita como fila, para Spring Data JPA.
 *
 * <p>Vive en el adaptador de persistencia y no en el dominio, a proposito: lleva anotaciones de JPA,
 * un constructor sin argumentos y campos mutables porque eso es lo que exige el mapeo. El dominio
 * ({@code co.fcv.citas.domain.appointment.Appointment}) no tiene ninguna de esas tres cosas y no sabe
 * que esta clase existe.
 *
 * <p>{@code statusId} es el identificador numerico de {@code appointment_statuses} y no el enum: la
 * traduccion entre codigo e identificador la hace {@link AppointmentStatusCatalog}. Mapearlo como
 * {@code @Enumerated} obligaria a que la columna guardase el nombre, y el esquema 3FN usa una tabla
 * de catalogo.
 */
@Entity
@Table(name = "appointments")
class AppointmentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "patient_user_id", nullable = false)
  private Long patientUserId;

  @Column(name = "professional_id", nullable = false)
  private Long professionalId;

  @Column(name = "location_id", nullable = false)
  private Short locationId;

  @Column(name = "specialty_id", nullable = false)
  private Short specialtyId;

  @Column(name = "status_id", nullable = false)
  private Short statusId;

  @Column(name = "reason", length = 500)
  private String reason;

  @Column(name = "scheduled_start_at", nullable = false)
  private LocalDateTime scheduledStartAt;

  @Column(name = "scheduled_end_at", nullable = false)
  private LocalDateTime scheduledEndAt;

  @Column(name = "created_by_user_id", nullable = false)
  private Long createdByUserId;

  @Column(name = "approved_by_user_id")
  private Long approvedByUserId;

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  /** Lo pone la base; se mapea para que {@code validate} no se queje de una columna sin atributo. */
  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", insertable = false)
  private Instant updatedAt;

  protected AppointmentEntity() {}

  AppointmentEntity(Long patientUserId, Long professionalId, Short locationId, Short specialtyId,
                    Short statusId, LocalDateTime start, LocalDateTime end, Long createdByUserId) {
    this.patientUserId = patientUserId;
    this.professionalId = professionalId;
    this.locationId = locationId;
    this.specialtyId = specialtyId;
    this.statusId = statusId;
    this.scheduledStartAt = start;
    this.scheduledEndAt = end;
    this.createdByUserId = createdByUserId;
  }

  Long getId() { return id; }
  Long getPatientUserId() { return patientUserId; }
  Long getProfessionalId() { return professionalId; }
  Short getLocationId() { return locationId; }
  Short getSpecialtyId() { return specialtyId; }
  Short getStatusId() { return statusId; }
  String getReason() { return reason; }
  LocalDateTime getScheduledStartAt() { return scheduledStartAt; }
  LocalDateTime getScheduledEndAt() { return scheduledEndAt; }

  void applyDecision(Short statusId, String reason, Long decidedByUserId, LocalDateTime decidedAt) {
    this.statusId = statusId;
    this.reason = reason;
    this.approvedByUserId = decidedByUserId;
    this.approvedAt = decidedAt;
  }

  void applyStatus(Short statusId) { this.statusId = statusId; }

  void reschedule(LocalDateTime start, LocalDateTime end, Short locationId) {
    this.scheduledStartAt = start;
    this.scheduledEndAt = end;
    this.locationId = locationId;
  }
}
