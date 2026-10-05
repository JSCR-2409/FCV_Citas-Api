package co.fcv.citas.appointment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** HU-027 bandeja de solicitudes especializadas y HU-028 resolucion por ADMIN. */
@RestController
@RequestMapping("/api/v1/admin/specialized-requests")
public class SpecializedRequestAdminController {

  private final JdbcTemplate db;

  private final AppointmentStatusLog history;

  private final co.fcv.citas.integration.StatusChangeNotifier notifier;

  public SpecializedRequestAdminController(JdbcTemplate db, AppointmentStatusLog history, co.fcv.citas.integration.StatusChangeNotifier notifier) { this.db = db; this.history = history; this.notifier = notifier; }

  /**
   * Lista solo las citas en REQUESTED, con los datos que HU-021 CA-03 exige para decidir:
   * paciente, profesional, sede, especialidad, franja y duracion. Los filtros de HU-027 CA-02
   * son opcionales y se combinan entre si.
   */
  @GetMapping
  ResponseEntity<?> list(@RequestParam(required = false) Long locationId,
                         @RequestParam(required = false) Long professionalId,
                         @RequestParam(required = false) Long specialtyId,
                         @RequestParam(required = false) LocalDate date) {
    var sql = new StringBuilder(
        "SELECT a.id,a.patient_user_id,u.first_name,u.last_name,a.professional_id,p.professional_code,"
        + "a.location_id,l.name,a.specialty_id,s.name,s.appointment_duration_minutes,"
        + "a.scheduled_start_at,a.scheduled_end_at,st.code"
        + " FROM appointments a"
        + " JOIN appointment_statuses st ON st.id=a.status_id AND st.code='REQUESTED'"
        + " JOIN users u ON u.id=a.patient_user_id"
        + " JOIN professionals p ON p.id=a.professional_id"
        + " JOIN locations l ON l.id=a.location_id"
        + " JOIN specialties s ON s.id=a.specialty_id"
        + " WHERE 1=1");
    var args = new ArrayList<Object>();
    if (locationId != null) { sql.append(" AND a.location_id=?"); args.add(locationId); }
    if (professionalId != null) { sql.append(" AND a.professional_id=?"); args.add(professionalId); }
    if (specialtyId != null) { sql.append(" AND a.specialty_id=?"); args.add(specialtyId); }
    if (date != null) { sql.append(" AND CAST(a.scheduled_start_at AS DATE)=?"); args.add(date); }
    sql.append(" ORDER BY a.scheduled_start_at");

    var rows = db.query(sql.toString(), (r, n) -> {
      var item = new LinkedHashMap<String, Object>();
      item.put("id", r.getLong(1));
      item.put("patientUserId", r.getLong(2));
      item.put("patientName", r.getString(3) + " " + r.getString(4));
      item.put("professionalId", r.getLong(5));
      item.put("professionalCode", r.getString(6));
      item.put("locationId", r.getLong(7));
      item.put("locationName", r.getString(8));
      item.put("specialtyId", r.getLong(9));
      item.put("specialtyName", r.getString(10));
      item.put("durationMinutes", r.getInt(11));
      // getObject(..., LocalDateTime.class) evita el desplazamiento que introduce getTimestamp()
      // al convertir entre el serverTimezone del JDBC y la zona de la JVM.
      item.put("startAt", r.getObject(12, LocalDateTime.class));
      item.put("endAt", r.getObject(13, LocalDateTime.class));
      item.put("status", r.getString(14));
      return item;
    }, args.toArray());
    return ResponseEntity.ok(rows);
  }

  /**
   * HU-028. Solo resuelve citas que siguen en REQUESTED: una cita ya resuelta, o una general que
   * nunca paso por esta bandeja, devuelve 409 sin transicion y sin liberar franjas.
   */
  @PatchMapping("/{id}")
  @Transactional
  ResponseEntity<?> decide(@PathVariable long id, @RequestBody Decision d, Authentication a) {
    if (d == null || (!"APPROVED".equals(d.status()) && !"REJECTED".equals(d.status()))) {
      return ResponseEntity.badRequest().body(Map.of("message", "decisión inválida"));
    }
    if ("REJECTED".equals(d.status()) && (d.reason() == null || d.reason().isBlank())) {
      return ResponseEntity.badRequest().body(Map.of("message", "motivo requerido"));
    }
    if (db.queryForObject("SELECT COUNT(*) FROM appointments WHERE id=?", Integer.class, id) == 0) {
      return ResponseEntity.notFound().build();
    }
    // La guarda es la condicion del estado, no un JOIN suelto: sin ella cualquier cita podia
    // redecidirse y perder su franja.
    int pending = db.queryForObject(
        "SELECT COUNT(*) FROM appointments a JOIN appointment_statuses s ON s.id=a.status_id"
        + " WHERE a.id=? AND s.code='REQUESTED'", Integer.class, id);
    if (pending == 0) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "la solicitud ya fue resuelta"));
    }

    boolean approved = "APPROVED".equals(d.status());
    db.update("UPDATE appointments SET status_id=(SELECT id FROM appointment_statuses WHERE code=?),"
        + "reason=?,"
        + "approved_by_user_id=?,approved_at=?,updated_at=CURRENT_TIMESTAMP"
        + " WHERE id=? AND status_id=(SELECT id FROM appointment_statuses WHERE code='REQUESTED')",
        d.status(),
        d.reason(),
        approved ? Long.parseLong(a.getName()) : null,
        approved ? LocalDateTime.now() : null,
        id);

    if (!approved) {
      db.update("UPDATE professional_slots SET appointment_id=NULL WHERE appointment_id=?", id);
    }
    // HU-031: la decision administrativa se audita con su motivo. En el rechazo el motivo es
    // obligatorio por RN-04, de modo que el historial siempre explica por que se rechazo.
    history.record(id, d.status(), Long.parseLong(a.getName()), AppointmentStatusLog.SOURCE_ADMIN, d.reason());
    // HU-033: cita especializada aprobada o rechazada, primer evento de WF-002.
    notifier.publish("APPOINTMENT_DECIDED", id, d.status(), d.reason());
    return ResponseEntity.ok(Map.of("id", id, "status", d.status(), "reason", d.reason() == null ? "" : d.reason()));
  }

  record Decision(String status, String reason) {}
}
