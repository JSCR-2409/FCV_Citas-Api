package co.fcv.citas.appointment;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * HU-031 / RF-19 — Consulta del historial de cambios de estado.
 *
 * <p>Vive bajo {@code /api/v1/admin} porque la auditoria es una funcion de operacion: expone quien
 * decidio sobre una cita, y eso no es informacion del paciente. La ruta queda cubierta por la regla
 * {@code hasRole("ADMIN")} de SecurityConfig sin necesidad de una anotacion por metodo.
 */
@RestController
@RequestMapping("/api/v1/admin/appointment-history")
public class AppointmentAuditController {

  private final JdbcTemplate db;

  public AppointmentAuditController(JdbcTemplate db) { this.db = db; }

  /**
   * CA-01 y CA-02. Sin filtros devuelve las transiciones mas recientes; {@code appointmentId}
   * reconstruye la vida completa de una cita en orden cronologico.
   */
  @GetMapping
  ResponseEntity<?> history(@RequestParam(required = false) Long appointmentId,
                            @RequestParam(required = false) String from,
                            @RequestParam(required = false) String to,
                            @RequestParam(required = false) Integer limit) {
    var sql = new StringBuilder(
        "SELECT h.id,h.appointment_id appointmentId,s.code statusCode,h.change_source changeSource,"
        + "h.reason,h.changed_at changedAt,h.changed_by_user_id actorId,"
        + "u.first_name actorNames,u.last_name actorSurnames"
        + " FROM appointment_status_history h"
        + " JOIN appointment_statuses s ON s.id=h.status_id"
        + " LEFT JOIN users u ON u.id=h.changed_by_user_id WHERE 1=1");
    var args = new java.util.ArrayList<Object>();
    if (appointmentId != null) { sql.append(" AND h.appointment_id=?"); args.add(appointmentId); }
    if (from != null && !from.isBlank()) { sql.append(" AND h.changed_at>=?"); args.add(java.time.LocalDate.parse(from.trim()).atStartOfDay()); }
    if (to != null && !to.isBlank()) { sql.append(" AND h.changed_at<?"); args.add(java.time.LocalDate.parse(to.trim()).plusDays(1).atStartOfDay()); }
    // Cronologico ascendente cuando se pide una cita concreta, porque lo que interesa es la
    // secuencia; descendente en la vista general, donde lo relevante es lo ultimo que paso.
    sql.append(appointmentId != null ? " ORDER BY h.changed_at ASC, h.id ASC" : " ORDER BY h.changed_at DESC, h.id DESC");

    int cap = limit == null ? 200 : Math.min(Math.max(limit, 1), 500);
    sql.append(" LIMIT ").append(cap);

    var rows = db.query(sql.toString(), (rs, n) -> {
      var item = new LinkedHashMap<String, Object>();
      item.put("id", rs.getLong("id"));
      item.put("appointmentId", rs.getLong("appointmentId"));
      item.put("statusCode", rs.getString("statusCode"));
      item.put("changeSource", rs.getString("changeSource"));
      item.put("reason", rs.getString("reason"));
      item.put("changedAt", rs.getObject("changedAt", LocalDateTime.class));
      long actor = rs.getLong("actorId");
      // El actor puede no existir: una transicion SYSTEM no tiene responsable humano, y el
      // contrato lo expresa con null en lugar de inventar un usuario.
      item.put("actorId", rs.wasNull() ? null : actor);
      String names = rs.getString("actorNames");
      item.put("actorName", names == null ? null : names + " " + rs.getString("actorSurnames"));
      return item;
    }, args.toArray());

    return ResponseEntity.ok(Map.of("items", rows, "count", rows.size()));
  }
}
