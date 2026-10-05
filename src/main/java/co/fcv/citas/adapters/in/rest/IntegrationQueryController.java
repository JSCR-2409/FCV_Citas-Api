package co.fcv.citas.adapters.in.rest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * HU-032 y HU-034 — Lecturas que consumen las automatizaciones n8n.
 *
 * <p>Son estrictamente de lectura: ningun metodo escribe, de modo que CA-03 de HU-032 —"no cambia
 * estado, reserva ni reglas de negocio"— se cumple por construccion y no por disciplina.
 *
 * <p>DECISION sobre los datos expuestos: solo lo que el recordatorio necesita para redactarse. El
 * numero de documento y el telefono no viajan; el correo si, porque es el destinatario del envio.
 * Todos los datos del laboratorio son sinteticos, conforme al PRD.
 */
@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationQueryController {

  /**
   * DECISION sobre el criterio de "proxima" (T-01 de HU-032): ventana parametrica con 24 horas por
   * defecto, que es el valor que sugiere la especificacion de WF-001. No se deduce en el workflow:
   * el criterio vive en el contrato, de modo que cambiarlo no exige reeditar el JSON.
   */
  private static final int DEFAULT_WINDOW_HOURS = 24;
  private static final int MAX_WINDOW_HOURS = 24 * 14;

  private final JdbcTemplate db;

  public IntegrationQueryController(JdbcTemplate db) { this.db = db; }

  /**
   * CA-01. Citas APPROVED que comienzan dentro de la ventana. Excluye lo que WF-001 prohibe
   * recordar: una cita CANCELLED o REJECTED no se selecciona porque el filtro es por codigo de
   * estado, no por ausencia de cancelacion.
   */
  @GetMapping("/appointments/upcoming")
  ResponseEntity<?> upcoming(@RequestParam(required = false) Integer withinHours) {
    int window = withinHours == null ? DEFAULT_WINDOW_HOURS
        : Math.min(Math.max(withinHours, 1), MAX_WINDOW_HOURS);
    LocalDateTime from = LocalDateTime.now();
    LocalDateTime to = from.plusHours(window);

    var items = db.query(
        "SELECT a.id,a.scheduled_start_at startAt,a.scheduled_end_at endAt,"
        + "u.first_name patientNames,u.last_name patientSurnames,u.email patientEmail,"
        + "d.first_name doctorNames,d.last_name doctorSurnames,"
        + "sp.name specialtyName,sp.appointment_duration_minutes durationMinutes,"
        + "l.name locationName,l.address locationAddress"
        + " FROM appointments a"
        + " JOIN appointment_statuses st ON st.id=a.status_id"
        + " JOIN users u ON u.id=a.patient_user_id"
        + " JOIN professionals pr ON pr.id=a.professional_id"
        + " JOIN users d ON d.id=pr.user_id"
        + " JOIN specialties sp ON sp.id=a.specialty_id"
        + " JOIN locations l ON l.id=a.location_id"
        + " WHERE st.code='APPROVED' AND a.scheduled_start_at>=? AND a.scheduled_start_at<?"
        + " ORDER BY a.scheduled_start_at ASC", (rs, n) -> {
          var row = new LinkedHashMap<String, Object>();
          row.put("appointmentId", rs.getLong("id"));
          row.put("startAt", rs.getObject("startAt", LocalDateTime.class));
          row.put("endAt", rs.getObject("endAt", LocalDateTime.class));
          row.put("patientName", rs.getString("patientNames") + " " + rs.getString("patientSurnames"));
          row.put("patientEmail", rs.getString("patientEmail"));
          row.put("professionalName", rs.getString("doctorNames") + " " + rs.getString("doctorSurnames"));
          row.put("specialtyName", rs.getString("specialtyName"));
          row.put("durationMinutes", rs.getInt("durationMinutes"));
          row.put("locationName", rs.getString("locationName"));
          row.put("locationAddress", rs.getString("locationAddress"));
          return row;
        }, from, to);

    var body = new LinkedHashMap<String, Object>();
    body.put("generatedAt", from);
    body.put("windowHours", window);
    body.put("windowEnd", to);
    body.put("count", items.size());
    body.put("items", items);
    return ResponseEntity.ok(body);
  }

  /**
   * HU-034 — Resumen operativo del dia, agrupado por sede, estado y especialidad. Es el insumo de
   * WF-003. Devuelve conteos, no citas: un resumen no necesita datos de ningun paciente.
   */
  @GetMapping("/daily-summary")
  ResponseEntity<?> dailySummary(@RequestParam(required = false) String date) {
    LocalDate day = date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date.trim());
    LocalDateTime from = day.atStartOfDay();
    LocalDateTime to = day.plusDays(1).atStartOfDay();

    var byLocationAndStatus = db.query(
        "SELECT l.name locationName,st.code statusCode,COUNT(*) total"
        + " FROM appointments a"
        + " JOIN appointment_statuses st ON st.id=a.status_id"
        + " JOIN locations l ON l.id=a.location_id"
        + " WHERE a.scheduled_start_at>=? AND a.scheduled_start_at<?"
        + " GROUP BY l.name,st.code ORDER BY l.name,st.code", (rs, n) -> Map.of(
            "locationName", rs.getString("locationName"),
            "status", rs.getString("statusCode"),
            "total", rs.getInt("total")), from, to);

    var bySpecialty = db.query(
        "SELECT sp.name specialtyName,COUNT(*) total"
        + " FROM appointments a JOIN specialties sp ON sp.id=a.specialty_id"
        + " WHERE a.scheduled_start_at>=? AND a.scheduled_start_at<?"
        + " GROUP BY sp.name ORDER BY total DESC,sp.name", (rs, n) -> Map.of(
            "specialtyName", rs.getString("specialtyName"),
            "total", rs.getInt("total")), from, to);

    var body = new LinkedHashMap<String, Object>();
    body.put("date", day);
    body.put("total", byLocationAndStatus.stream().mapToInt(r -> (Integer) r.get("total")).sum());
    body.put("byLocationAndStatus", byLocationAndStatus);
    body.put("bySpecialty", bySpecialty);
    return ResponseEntity.ok(body);
  }
}
