package co.fcv.citas.appointment;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * HU-024 — Solicitar reprogramación.
 *
 * La retención doble que exige RN-10 se consigue sin columnas nuevas: mientras la solicitud está
 * PENDING, las franjas propuestas se marcan con el mismo `appointment_id` que las originales, de
 * modo que la cita retiene ambas y nadie más puede tomarlas. Qué franja es la original y cuál la
 * propuesta se deduce de `previous_*` y `requested_*` de la solicitud, que es lo que luego permite
 * liberar exactamente el lado que corresponda al decidir.
 */
@RestController
@RequestMapping("/api/v1/me")
public class PatientReschedulingController {

  private final JdbcTemplate db;

  public PatientReschedulingController(JdbcTemplate db) { this.db = db; }

  @PostMapping("/appointments/{appointmentId}/reschedule-requests")
  @Transactional
  ResponseEntity<?> request(@PathVariable long appointmentId, @RequestBody SlotRequest body, Authentication auth) {
    if (body == null || body.slotId() == null) return bad("datos incompletos");
    long patient = Long.parseLong(auth.getName());

    // Solo una cita propia, APPROVED y futura puede reprogramarse (CA-01 y CA-03).
    var appointments = db.query(
        "SELECT a.professional_id,a.location_id,a.specialty_id,a.scheduled_start_at,a.scheduled_end_at,"
        + "sp.appointment_duration_minutes"
        + " FROM appointments a"
        + " JOIN appointment_statuses st ON st.id=a.status_id AND st.code='APPROVED'"
        + " JOIN specialties sp ON sp.id=a.specialty_id"
        + " WHERE a.id=? AND a.patient_user_id=? AND a.scheduled_start_at>NOW()",
        (rs, n) -> new Object[] {rs.getLong(1), rs.getLong(2), rs.getLong(3),
            rs.getObject(4, LocalDateTime.class), rs.getObject(5, LocalDateTime.class), rs.getInt(6)},
        appointmentId, patient);
    if (appointments.isEmpty()) {
      return conflict("La cita no puede reprogramarse: debe ser propia, aprobada y futura");
    }
    var appointment = appointments.get(0);
    long professionalId = (Long) appointment[0];
    long specialtyId = (Long) appointment[2];
    var previousStart = (LocalDateTime) appointment[3];
    var previousEnd = (LocalDateTime) appointment[4];
    int duration = (Integer) appointment[5];

    if (pendingRequestsFor(appointmentId) > 0) {
      return conflict("Ya existe una solicitud de reprogramación pendiente para esta cita");
    }

    // La franja propuesta debe ser del mismo profesional y de la misma especialidad: cambiar de
    // profesional es una cita nueva, no una reprogramación (CA-03).
    var slots = db.query(
        "SELECT ps.start_at,ab.location_id FROM professional_slots ps"
        + " JOIN availability_blocks ab ON ab.id=ps.availability_block_id AND ab.active=TRUE"
        + " JOIN professionals p ON p.id=ab.professional_id AND p.active=TRUE"
        + " JOIN professional_specialties psp ON psp.professional_id=ab.professional_id"
        + "   AND psp.specialty_id=? AND psp.active=TRUE"
        + " WHERE ps.id=? AND ab.professional_id=? AND (ps.appointment_id IS NULL OR ps.appointment_id=?)",
        (rs, n) -> new Object[] {rs.getObject(1, LocalDateTime.class), rs.getLong(2)},
        specialtyId, body.slotId(), professionalId, appointmentId);
    if (slots.isEmpty()) {
      return conflict("La franja propuesta no está disponible para el mismo profesional y especialidad");
    }
    var requestedStart = (LocalDateTime) slots.get(0)[0];
    long locationId = (Long) slots.get(0)[1];
    var requestedEnd = requestedStart.plusMinutes(duration);

    if (requestedStart.equals(previousStart)) return bad("La franja propuesta es la misma de la cita");
    if (!requestedStart.isAfter(LocalDateTime.now())) return bad("La franja propuesta debe ser futura");

    // Debe caber la duración completa: libre, o ya retenida por esta misma cita cuando la nueva
    // franja se solapa con la original.
    int needed = duration / 30;
    int usable = db.queryForObject(
        "SELECT COUNT(*) FROM professional_slots ps"
        + " JOIN availability_blocks ab ON ab.id=ps.availability_block_id"
        + " WHERE ab.professional_id=? AND ab.location_id=? AND ps.start_at>=? AND ps.start_at<?"
        + "   AND (ps.appointment_id IS NULL OR ps.appointment_id=?)",
        Integer.class, professionalId, locationId, requestedStart, requestedEnd, appointmentId);
    if (usable != needed) return conflict("La franja propuesta no completa la duración requerida");

    var key = new org.springframework.jdbc.support.GeneratedKeyHolder();
    db.update(c -> {
      var p = c.prepareStatement(
          "INSERT INTO reschedule_requests(appointment_id,requested_by_user_id,requested_location_id,"
          + "status_id,previous_start_at,previous_end_at,requested_start_at,requested_end_at)"
          + " SELECT ?,?,?,s.id,?,?,?,? FROM reschedule_request_statuses s WHERE s.code='PENDING'",
          new String[] {"id"});
      p.setLong(1, appointmentId);
      p.setLong(2, patient);
      p.setLong(3, locationId);
      p.setObject(4, previousStart);
      p.setObject(5, previousEnd);
      p.setObject(6, requestedStart);
      p.setObject(7, requestedEnd);
      return p;
    }, key);
    long requestId = Objects.requireNonNull(key.getKey()).longValue();

    // Retiene la franja propuesta sin tocar la original (CA-02).
    db.update("UPDATE professional_slots SET appointment_id=? WHERE appointment_id IS NULL"
        + " AND start_at>=? AND start_at<? AND availability_block_id IN"
        + " (SELECT id FROM availability_blocks WHERE professional_id=? AND location_id=?)",
        appointmentId, requestedStart, requestedEnd, professionalId, locationId);
    int held = db.queryForObject(
        "SELECT COUNT(*) FROM professional_slots ps"
        + " JOIN availability_blocks ab ON ab.id=ps.availability_block_id"
        + " WHERE ab.professional_id=? AND ps.start_at>=? AND ps.start_at<? AND ps.appointment_id=?",
        Integer.class, professionalId, requestedStart, requestedEnd, appointmentId);
    if (held != needed) throw new IllegalStateException("conflicto de concurrencia al retener la franja");

    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
        "id", requestId, "appointmentId", appointmentId, "status", "PENDING",
        "previousStartAt", previousStart, "previousEndAt", previousEnd,
        "requestedStartAt", requestedStart, "requestedEndAt", requestedEnd));
  }

  /** El paciente consulta sus propias solicitudes, para ver el estado y el motivo del rechazo. */
  @GetMapping("/reschedule-requests")
  ResponseEntity<?> mine(Authentication auth) {
    return ResponseEntity.ok(db.query(
        "SELECT rr.id,rr.appointment_id,s.code,rr.previous_start_at,rr.previous_end_at,"
        + "rr.requested_start_at,rr.requested_end_at,rr.decision_reason,"
        + "rr.patient_action_after_rejection,sp.name,l.name"
        + " FROM reschedule_requests rr"
        + " JOIN reschedule_request_statuses s ON s.id=rr.status_id"
        + " JOIN appointments a ON a.id=rr.appointment_id"
        + " JOIN specialties sp ON sp.id=a.specialty_id"
        + " JOIN locations l ON l.id=rr.requested_location_id"
        + " WHERE rr.requested_by_user_id=? ORDER BY rr.created_at DESC",
        (r, n) -> {
          var item = new LinkedHashMap<String, Object>();
          item.put("id", r.getLong(1));
          item.put("appointmentId", r.getLong(2));
          item.put("status", r.getString(3));
          item.put("previousStartAt", r.getObject(4, LocalDateTime.class));
          item.put("previousEndAt", r.getObject(5, LocalDateTime.class));
          item.put("requestedStartAt", r.getObject(6, LocalDateTime.class));
          item.put("requestedEndAt", r.getObject(7, LocalDateTime.class));
          item.put("decisionReason", r.getString(8));
          item.put("patientAction", r.getString(9));
          item.put("specialtyName", r.getString(10));
          item.put("locationName", r.getString(11));
          return item;
        },
        Long.parseLong(auth.getName())));
  }

  /**
   * Tras un rechazo el paciente decide si conserva la cita original o la cancela (HU-030 CA-02).
   * Conservarla no cambia la cita; cancelarla reutiliza la misma regla que la cancelación normal.
   */
  @PatchMapping("/reschedule-requests/{requestId}/action")
  @Transactional
  ResponseEntity<?> decideAfterRejection(@PathVariable long requestId, @RequestBody ActionRequest body,
                                         Authentication auth) {
    if (body == null || (!"KEEP_APPOINTMENT".equals(body.action()) && !"CANCEL_APPOINTMENT".equals(body.action()))) {
      return bad("acción inválida");
    }
    long patient = Long.parseLong(auth.getName());
    var rows = db.query(
        "SELECT rr.appointment_id FROM reschedule_requests rr"
        + " JOIN reschedule_request_statuses s ON s.id=rr.status_id AND s.code='REJECTED'"
        + " WHERE rr.id=? AND rr.requested_by_user_id=?",
        (rs, n) -> rs.getLong(1), requestId, patient);
    if (rows.isEmpty()) return conflict("Solo se puede responder a una solicitud propia ya rechazada");
    long appointmentId = rows.get(0);

    db.update("UPDATE reschedule_requests SET patient_action_after_rejection=? WHERE id=?",
        body.action(), requestId);

    if ("CANCEL_APPOINTMENT".equals(body.action())) {
      int moved = db.update("UPDATE appointments SET status_id="
          + "(SELECT id FROM appointment_statuses WHERE code='CANCELLED'),updated_at=CURRENT_TIMESTAMP"
          + " WHERE id=? AND patient_user_id=? AND scheduled_start_at>NOW()"
          + " AND status_id IN (SELECT id FROM appointment_statuses WHERE code IN ('APPROVED','REQUESTED'))",
          appointmentId, patient);
      if (moved == 0) return conflict("La cita ya no puede cancelarse");
      db.update("UPDATE professional_slots SET appointment_id=NULL WHERE appointment_id=?", appointmentId);
    }
    return ResponseEntity.ok(Map.of("id", requestId, "action", body.action(), "appointmentId", appointmentId));
  }

  private int pendingRequestsFor(long appointmentId) {
    return db.queryForObject("SELECT COUNT(*) FROM reschedule_requests rr"
        + " JOIN reschedule_request_statuses s ON s.id=rr.status_id AND s.code='PENDING'"
        + " WHERE rr.appointment_id=?", Integer.class, appointmentId);
  }

  private ResponseEntity<Map<String,String>> bad(String message) {
    return ResponseEntity.badRequest().body(Map.of("message", message));
  }

  private ResponseEntity<Map<String,String>> conflict(String message) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", message));
  }

  record SlotRequest(Long slotId) {}
  record ActionRequest(String action) {}
}
