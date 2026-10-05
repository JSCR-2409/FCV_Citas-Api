package co.fcv.citas.adapters.in.rest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * HU-025 y HU-026 — Agenda propia del PROFESSIONAL y cierre de atencion.
 *
 * <p>RF-16 limita lo que el profesional puede ver a "sus propias citas", de modo que el filtro por
 * profesional no es un parametro: se deriva del token. Un identificador de profesional en la
 * peticion seria exactamente el agujero que la regla prohibe.
 */
@RestController
@RequestMapping("/api/v1/professional/appointments")
public class ProfessionalAgendaController {

  private final JdbcTemplate db;
  private final co.fcv.citas.application.appointment.CloseAttentionUseCase closeAttentionUseCase;

  public ProfessionalAgendaController(JdbcTemplate db,
      co.fcv.citas.application.appointment.CloseAttentionUseCase closeAttentionUseCase) {
    this.db = db;
    this.closeAttentionUseCase = closeAttentionUseCase;
  }

  /**
   * HU-025 CA-01 a CA-03. Rango por dia o semana y filtro opcional por sede. El rango por defecto
   * es el dia de hoy, que es la vista que necesita quien va a atender.
   */
  @GetMapping
  ResponseEntity<?> agenda(Authentication auth,
                           @RequestParam(required = false) LocalDate from,
                           @RequestParam(required = false) LocalDate to,
                           @RequestParam(required = false) Long locationId,
                           @RequestParam(required = false) String status) {
    Long professionalId = professionalOf(auth);
    if (professionalId == null) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "profesional inactivo o inexistente"));
    }
    LocalDate desde = from != null ? from : LocalDate.now();
    LocalDate hasta = to != null ? to : desde;

    var sql = new StringBuilder(
        "SELECT a.id,a.scheduled_start_at startAt,a.scheduled_end_at endAt,st.code statusCode,"
        + "u.first_name patientNames,u.last_name patientSurnames,u.document_type documentType,"
        + "u.document_number documentNumber,sp.name specialtyName,sp.appointment_duration_minutes durationMinutes,"
        + "l.id locationId,l.name locationName"
        + " FROM appointments a"
        + " JOIN appointment_statuses st ON st.id=a.status_id"
        + " JOIN users u ON u.id=a.patient_user_id"
        + " JOIN specialties sp ON sp.id=a.specialty_id"
        + " JOIN locations l ON l.id=a.location_id"
        + " WHERE a.professional_id=? AND a.scheduled_start_at>=? AND a.scheduled_start_at<?");
    var args = new java.util.ArrayList<Object>();
    args.add(professionalId);
    args.add(desde.atStartOfDay());
    args.add(hasta.plusDays(1).atStartOfDay());

    if (locationId != null) { sql.append(" AND a.location_id=?"); args.add(locationId); }
    if (status != null && !status.isBlank()) {
      var codes = java.util.Arrays.stream(status.split(",")).map(String::trim)
          .filter(c -> !c.isEmpty()).map(c -> c.toUpperCase(java.util.Locale.ROOT)).toList();
      if (!codes.isEmpty()) {
        sql.append(" AND st.code IN (").append("?,".repeat(codes.size() - 1)).append("?)");
        args.addAll(codes);
      }
    } else {
      // RF-16 habla de la agenda de citas APPROVED: es lo que el profesional va a atender. Las
      // demas siguen siendo consultables pasando status explicitamente.
      sql.append(" AND st.code IN ('APPROVED','COMPLETED','NO_SHOW')");
    }
    sql.append(" ORDER BY a.scheduled_start_at ASC");

    var rows = db.query(sql.toString(), (rs, n) -> {
      var item = new LinkedHashMap<String, Object>();
      item.put("id", rs.getLong("id"));
      item.put("startAt", rs.getObject("startAt", LocalDateTime.class));
      item.put("endAt", rs.getObject("endAt", LocalDateTime.class));
      item.put("status", rs.getString("statusCode"));
      item.put("patientName", rs.getString("patientNames") + " " + rs.getString("patientSurnames"));
      // Documento y no email ni telefono: identifica al paciente en la consulta sin convertir la
      // agenda en un directorio de contacto de pacientes ajenos a la atencion.
      item.put("patientDocument", rs.getString("documentType") + " " + rs.getString("documentNumber"));
      item.put("specialtyName", rs.getString("specialtyName"));
      item.put("durationMinutes", rs.getInt("durationMinutes"));
      item.put("locationId", rs.getLong("locationId"));
      item.put("locationName", rs.getString("locationName"));
      return item;
    }, args.toArray());

    return ResponseEntity.ok(Map.of("from", desde, "to", hasta, "items", rows, "count", rows.size()));
  }

  /**
   * HU-026 CA-01 a CA-03. Cierra la atencion como COMPLETED o NO_SHOW.
   *
   * <p>Las tres condiciones son indivisibles y van en la guarda del UPDATE: la cita es del
   * profesional del token, esta APPROVED y su hora de inicio ya paso. Comprobarlas por separado
   * dejaria una ventana entre la lectura y la escritura.
   */
  @PatchMapping("/{id}/attention")
  @Transactional
  ResponseEntity<?> closeAttention(@PathVariable long id, @RequestBody Outcome body, Authentication auth) {
    Long professionalId = professionalOf(auth);
    if (professionalId == null) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "profesional inactivo o inexistente"));
    }
    // 404 y no 403 cuando la cita es de otro profesional: responder distinto permitiria descubrir
    // que citas existen en la agenda de otro. Esta comprobacion es del adaptador porque decide un
    // codigo HTTP, no una regla de negocio.
    if (!closeAttentionUseCase.isAttendedBy(id, professionalId)) {
      return ResponseEntity.notFound().build();
    }
    var outcome = body == null ? null : body.outcome();
    // El dominio valida el resultado; convertirlo aqui requiere que sea un codigo conocido.
    co.fcv.citas.domain.appointment.AppointmentStatus parsed;
    try {
      parsed = co.fcv.citas.domain.appointment.AppointmentStatus.of(outcome);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("message", "resultado debe ser COMPLETED o NO_SHOW"));
    }
    var closed = closeAttentionUseCase.close(id, parsed, professionalId,
        Long.parseLong(auth.getName()), body.notes());
    return ResponseEntity.ok(Map.of("id", id, "status", closed.status().name()));
  }

  /** Profesional activo asociado al usuario del token, o {@code null} si no lo hay. */
  private Long professionalOf(Authentication auth) {
    var ids = db.query("SELECT p.id FROM professionals p WHERE p.user_id=? AND p.active=TRUE",
        (rs, n) -> rs.getLong(1), Long.parseLong(auth.getName()));
    return ids.isEmpty() ? null : ids.get(0);
  }

  record Outcome(String outcome, String notes) {}
}
