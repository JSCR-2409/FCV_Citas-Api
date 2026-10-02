package co.fcv.citas.appointment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * HU-029 bandeja de reprogramaciones y HU-030 resolución por ADMIN.
 *
 * Mientras la solicitud está PENDING la cita retiene las dos franjas. Aprobar libera la original y
 * mueve la cita; rechazar libera la propuesta y deja la cita intacta. En ambos casos se libera solo
 * el lado que corresponde, excluyendo el solape cuando las dos franjas comparten slots.
 */
@RestController
@RequestMapping("/api/v1/admin/reschedule-requests")
public class RescheduleRequestAdminController {

  private final JdbcTemplate db;

  public RescheduleRequestAdminController(JdbcTemplate db) { this.db = db; }

  @GetMapping
  ResponseEntity<?> list(@RequestParam(required = false) Long locationId,
                         @RequestParam(required = false) Long professionalId,
                         @RequestParam(required = false) Long specialtyId,
                         @RequestParam(required = false) LocalDate date) {
    var sql = new StringBuilder(
        "SELECT rr.id,rr.appointment_id,u.first_name,u.last_name,a.professional_id,p.professional_code,"
        + "pu.first_name,pu.last_name,a.specialty_id,sp.name,sp.appointment_duration_minutes,"
        + "rr.requested_location_id,l.name,rr.previous_start_at,rr.previous_end_at,"
        + "rr.requested_start_at,rr.requested_end_at,s.code"
        + " FROM reschedule_requests rr"
        + " JOIN reschedule_request_statuses s ON s.id=rr.status_id AND s.code='PENDING'"
        + " JOIN appointments a ON a.id=rr.appointment_id"
        + " JOIN users u ON u.id=rr.requested_by_user_id"
        + " JOIN professionals p ON p.id=a.professional_id"
        + " JOIN users pu ON pu.id=p.user_id"
        + " JOIN specialties sp ON sp.id=a.specialty_id"
        + " JOIN locations l ON l.id=rr.requested_location_id"
        + " WHERE 1=1");
    var args = new ArrayList<Object>();
    if (locationId != null) { sql.append(" AND rr.requested_location_id=?"); args.add(locationId); }
    if (professionalId != null) { sql.append(" AND a.professional_id=?"); args.add(professionalId); }
    if (specialtyId != null) { sql.append(" AND a.specialty_id=?"); args.add(specialtyId); }
    if (date != null) { sql.append(" AND CAST(rr.requested_start_at AS DATE)=?"); args.add(date); }
    sql.append(" ORDER BY rr.requested_start_at");

    var rows = db.query(sql.toString(), (r, n) -> {
      var item = new LinkedHashMap<String, Object>();
      item.put("id", r.getLong(1));
      item.put("appointmentId", r.getLong(2));
      item.put("patientName", r.getString(3) + " " + r.getString(4));
      item.put("professionalId", r.getLong(5));
      item.put("professionalCode", r.getString(6));
      item.put("professionalName", r.getString(7) + " " + r.getString(8));
      item.put("specialtyId", r.getLong(9));
      item.put("specialtyName", r.getString(10));
      item.put("durationMinutes", r.getInt(11));
      item.put("locationId", r.getLong(12));
      item.put("locationName", r.getString(13));
      item.put("previousStartAt", r.getObject(14, LocalDateTime.class));
      item.put("previousEndAt", r.getObject(15, LocalDateTime.class));
      item.put("requestedStartAt", r.getObject(16, LocalDateTime.class));
      item.put("requestedEndAt", r.getObject(17, LocalDateTime.class));
      item.put("status", r.getString(18));
      return item;
    }, args.toArray());
    return ResponseEntity.ok(rows);
  }

  @PatchMapping("/{id}")
  @Transactional
  ResponseEntity<?> decide(@PathVariable long id, @RequestBody Decision d, Authentication auth) {
    if (d == null || (!"APPROVED".equals(d.status()) && !"REJECTED".equals(d.status()))) {
      return ResponseEntity.badRequest().body(Map.of("message", "decisión inválida"));
    }
    if ("REJECTED".equals(d.status()) && (d.reason() == null || d.reason().isBlank())) {
      return ResponseEntity.badRequest().body(Map.of("message", "motivo requerido"));
    }
    if (db.queryForObject("SELECT COUNT(*) FROM reschedule_requests WHERE id=?", Integer.class, id) == 0) {
      return ResponseEntity.notFound().build();
    }

    // Solo una solicitud que siga PENDING puede decidirse: evita decisiones dobles y cambios
    // parciales (CA-03).
    var pending = db.query(
        "SELECT rr.appointment_id,a.professional_id,rr.requested_location_id,"
        + "rr.previous_start_at,rr.previous_end_at,rr.requested_start_at,rr.requested_end_at"
        + " FROM reschedule_requests rr"
        + " JOIN reschedule_request_statuses s ON s.id=rr.status_id AND s.code='PENDING'"
        + " JOIN appointments a ON a.id=rr.appointment_id"
        + " WHERE rr.id=?",
        (rs, n) -> new Object[] {rs.getLong(1), rs.getLong(2), rs.getLong(3),
            rs.getObject(4, LocalDateTime.class), rs.getObject(5, LocalDateTime.class),
            rs.getObject(6, LocalDateTime.class), rs.getObject(7, LocalDateTime.class)},
        id);
    if (pending.isEmpty()) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "la solicitud ya fue resuelta"));
    }
    var row = pending.get(0);
    long appointmentId = (Long) row[0];
    long professionalId = (Long) row[1];
    long locationId = (Long) row[2];
    var previousStart = (LocalDateTime) row[3];
    var previousEnd = (LocalDateTime) row[4];
    var requestedStart = (LocalDateTime) row[5];
    var requestedEnd = (LocalDateTime) row[6];

    boolean approved = "APPROVED".equals(d.status());
    if (approved) {
      // No se puede confirmar una propuesta cuya franja no esta efectivamente retenida por esta
      // cita: mover la cita ahi la dejaria sobre slots que otro paciente podria reservar (CA-03).
      int neededSlots = (int) (java.time.Duration.between(requestedStart, requestedEnd).toMinutes() / 30);
      int heldSlots = db.queryForObject(
          "SELECT COUNT(*) FROM professional_slots ps"
          + " JOIN availability_blocks ab ON ab.id=ps.availability_block_id"
          + " WHERE ab.professional_id=? AND ps.start_at>=? AND ps.start_at<? AND ps.appointment_id=?",
          Integer.class, professionalId, requestedStart, requestedEnd, appointmentId);
      if (heldSlots != neededSlots) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
            "message", "La franja propuesta ya no está retenida para esta cita; la solicitud no puede aprobarse"));
      }
      releaseExcept(appointmentId, professionalId, previousStart, previousEnd, requestedStart, requestedEnd);
      db.update("UPDATE appointments SET scheduled_start_at=?,scheduled_end_at=?,location_id=?,"
          + "updated_at=CURRENT_TIMESTAMP WHERE id=?",
          requestedStart, requestedEnd, locationId, appointmentId);
    } else {
      releaseExcept(appointmentId, professionalId, requestedStart, requestedEnd, previousStart, previousEnd);
    }

    db.update("UPDATE reschedule_requests SET status_id="
        + "(SELECT id FROM reschedule_request_statuses WHERE code=?),"
        + "decision_reason=?,decided_by_user_id=?,decided_at=? WHERE id=?",
        d.status(), d.reason(), Long.parseLong(auth.getName()), LocalDateTime.now(), id);

    return ResponseEntity.ok(Map.of("id", id, "appointmentId", appointmentId, "status", d.status(),
        "reason", d.reason() == null ? "" : d.reason()));
  }

  /**
   * Libera las franjas de la cita en el rango [from,to), salvo las que caen en [keepFrom,keepTo).
   * La exclusión importa cuando la franja propuesta se solapa con la original: sin ella, mover una
   * cita de 60 minutos media hora más tarde liberaría un slot que la cita sigue necesitando.
   */
  private void releaseExcept(long appointmentId, long professionalId,
                             LocalDateTime from, LocalDateTime to,
                             LocalDateTime keepFrom, LocalDateTime keepTo) {
    db.update("UPDATE professional_slots SET appointment_id=NULL"
        + " WHERE appointment_id=? AND start_at>=? AND start_at<? AND NOT (start_at>=? AND start_at<?)"
        + " AND availability_block_id IN (SELECT id FROM availability_blocks WHERE professional_id=?)",
        appointmentId, from, to, keepFrom, keepTo, professionalId);
  }

  record Decision(String status, String reason) {}
}
