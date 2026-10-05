package co.fcv.citas.adapters.in.rest;

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

  private final co.fcv.citas.application.appointment.DecideSpecializedRequestUseCase decideRequest;

  private final co.fcv.citas.adapters.out.notification.StatusChangeNotifier notifier;

  public SpecializedRequestAdminController(JdbcTemplate db, co.fcv.citas.application.appointment.DecideSpecializedRequestUseCase decideRequest, co.fcv.citas.adapters.out.notification.StatusChangeNotifier notifier) { this.db = db; this.decideRequest = decideRequest; this.notifier = notifier; }

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
  /**
   * HU-028. El adaptador ya no decide: traduce. Las reglas —decision valida, motivo obligatorio en el
   * rechazo por RN-04 y la guarda de que siga pendiente— viven en el dominio, y las violaciones las
   * convierte DomainRuleViolationHandler. Lo unico que queda aqui es el 404, que no es una regla de
   * negocio sino la ausencia del recurso.
   */
  @PatchMapping("/{id}")
  ResponseEntity<?> decide(@PathVariable long id, @RequestBody Decision d, Authentication a) {
    var decision = d == null ? null : d.status();
    if (decision == null || !("APPROVED".equals(decision) || "REJECTED".equals(decision))) {
      return ResponseEntity.badRequest().body(Map.of("message", "decisión inválida"));
    }
    try {
      var resolved = decideRequest.decide(id,
          co.fcv.citas.domain.appointment.AppointmentStatus.of(decision), d.reason(),
          Long.parseLong(a.getName()));
      // HU-033: primer evento de WF-002. Fuera del caso de uso: avisar a un sistema externo no es
      // parte de la regla de decidir.
      notifier.publish("APPOINTMENT_DECIDED", id, resolved.status().name(), resolved.reason());
      return ResponseEntity.ok(Map.of("id", id, "status", resolved.status().name(),
          "reason", resolved.reason() == null ? "" : resolved.reason()));
    } catch (co.fcv.citas.application.appointment.DecideSpecializedRequestUseCase.NotFound e) {
      return ResponseEntity.notFound().build();
    }
  }

  record Decision(String status, String reason) {}
}
