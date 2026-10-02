package co.fcv.citas.user;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class CurrentUserController {
  private final JdbcTemplate db;
  private final UserRepository users;
  public CurrentUserController(JdbcTemplate db, UserRepository users) { this.db = db; this.users = users; }

  @GetMapping
  Map<String,Object> profile(Authentication auth) {
    long id = Long.parseLong(auth.getName());
    // Las claves se construyen aqui en lugar de heredarse de queryForMap: ese metodo las toma de
    // los metadatos del driver, que las devuelve en mayusculas en H2 y en minusculas en MySQL, de
    // modo que la forma del JSON dependia del motor y el contrato no puede depender de eso.
    var profile = db.query(
        "SELECT u.id,u.first_name,u.last_name,u.document_type,u.document_number,u.email,u.phone,u.active"
        + " FROM users u WHERE u.id=?", (rs, n) -> {
          var row = new java.util.LinkedHashMap<String,Object>();
          row.put("id", rs.getLong(1));
          row.put("names", rs.getString(2));
          row.put("surnames", rs.getString(3));
          row.put("documentType", rs.getString(4));
          row.put("documentNumber", rs.getString(5));
          row.put("email", rs.getString(6));
          row.put("phone", rs.getString(7));
          row.put("active", rs.getBoolean(8));
          return row;
        }, id).get(0);
    // Todos los roles, no solo uno: user_roles es N:M. "role" es el de presentacion y usa el
    // mismo criterio que el token (orden alfabetico), para que ambos no se contradigan.
    var roles = db.queryForList("SELECT r.code FROM user_roles ur JOIN roles r ON r.id=ur.role_id"
        + " WHERE ur.user_id=? ORDER BY r.code", String.class, id);
    if (roles.isEmpty()) roles = java.util.List.of("USER");
    profile.put("roles", roles);
    profile.put("role", roles.get(0));
    return profile;
  }

  /**
   * HU-007 CA-02 y CA-03. Solo nombres, apellidos y telefono. Un campo no permitido que llegue en
   * el cuerpo se ignora por construccion: no se lee del JSON, de modo que no hay forma de colarlo.
   * El id del usuario viene del token, nunca del cuerpo ni de la ruta, asi que el ownership no
   * depende de lo que envie el cliente.
   */
  @PatchMapping
  @org.springframework.transaction.annotation.Transactional
  org.springframework.http.ResponseEntity<?> updateProfile(@RequestBody ProfileUpdate body, Authentication auth) {
    long id = Long.parseLong(auth.getName());
    String names = trimmedOrNull(body.names());
    String surnames = trimmedOrNull(body.surnames());
    String phone = trimmedOrNull(body.phone());
    if (names == null && surnames == null && phone == null) {
      return org.springframework.http.ResponseEntity.badRequest()
          .body(Map.of("message", "no hay cambios válidos: se admiten names, surnames y phone"));
    }
    var user = users.findById(id).filter(User::isActive).orElse(null);
    if (user == null) return org.springframework.http.ResponseEntity.notFound().build();
    user.updateContactDetails(names, surnames, phone);
    // saveAndFlush y no save: la respuesta se arma leyendo por JDBC, y JPA no vuelca hasta el
    // commit, de modo que con save() el PATCH devolvia los valores anteriores al cambio.
    users.saveAndFlush(user);
    return org.springframework.http.ResponseEntity.ok(profile(auth));
  }

  /** Una cadena en blanco no es un cambio: borrar el telefono dejaria el perfil incompleto. */
  private static String trimmedOrNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public record ProfileUpdate(String names, String surnames, String phone) {}

  /**
   * HU-022. Los filtros de estado y fecha se aplican en SQL, no en el cliente: la proyeccion debe
   * devolver solo lo que corresponde. {@code reason} viaja siempre porque es el motivo del rechazo
   * administrativo (CA-03) y la UI lo necesita para explicar un REJECTED.
   */
  @GetMapping("/appointments")
  Object appointments(Authentication auth,
                      @RequestParam(required = false) String status,
                      @RequestParam(required = false) String from,
                      @RequestParam(required = false) String to) {
    // professionalId y specialtyId son necesarios para reprogramar: la nueva franja debe ser del
    // mismo profesional y la misma especialidad, y la UI los usa para consultar disponibilidad.
    var sql = new StringBuilder("SELECT a.id,a.scheduled_start_at startAt,a.scheduled_end_at endAt,s.code status,a.reason,p.first_name names,p.last_name surnames,sp.name specialty,l.code locationCode,l.name locationName,a.professional_id professionalId,a.specialty_id specialtyId,sp.appointment_duration_minutes durationMinutes FROM appointments a JOIN appointment_statuses s ON s.id=a.status_id JOIN professionals pr ON pr.id=a.professional_id JOIN users p ON p.id=pr.user_id JOIN specialties sp ON sp.id=a.specialty_id JOIN locations l ON l.id=a.location_id WHERE a.patient_user_id=?");
    var args = new java.util.ArrayList<Object>();
    args.add(Long.parseLong(auth.getName()));
    if (status != null && !status.isBlank()) {
      // Varios estados separados por coma: la UI necesita "las activas" en una sola llamada.
      var codes = java.util.Arrays.stream(status.split(",")).map(String::trim)
          .filter(c -> !c.isEmpty()).map(c -> c.toUpperCase(java.util.Locale.ROOT)).toList();
      if (!codes.isEmpty()) {
        sql.append(" AND s.code IN (").append("?,".repeat(codes.size() - 1)).append("?)");
        args.addAll(codes);
      }
    }
    // El filtro de fecha es inclusivo en ambos extremos y se expresa sobre el dia completo: "to"
    // llega como fecha, asi que una cita de las 16:00 de ese dia debe quedar dentro.
    if (from != null && !from.isBlank()) { sql.append(" AND a.scheduled_start_at>=?"); args.add(java.time.LocalDate.parse(from.trim()).atStartOfDay()); }
    if (to != null && !to.isBlank()) { sql.append(" AND a.scheduled_start_at<?"); args.add(java.time.LocalDate.parse(to.trim()).plusDays(1).atStartOfDay()); }
    sql.append(" ORDER BY a.scheduled_start_at DESC");
    return db.query(sql.toString(), (rs,n)->{
      var item = new java.util.LinkedHashMap<String,Object>();
      item.put("id", rs.getLong("id"));
      item.put("startAt", rs.getObject("startAt", java.time.LocalDateTime.class));
      item.put("endAt", rs.getObject("endAt", java.time.LocalDateTime.class));
      item.put("status", rs.getString("status"));
      item.put("doctorName", rs.getString("names") + " " + rs.getString("surnames"));
      item.put("specialty", rs.getString("specialty"));
      item.put("facility", rs.getString("locationCode"));
      item.put("facilityFullName", rs.getString("locationName"));
      item.put("professionalId", rs.getLong("professionalId"));
      item.put("specialtyId", rs.getLong("specialtyId"));
      item.put("durationMinutes", rs.getInt("durationMinutes"));
      // CA-03: el motivo del rechazo administrativo. Va como null cuando no aplica, en lugar de
      // omitirse, para que el cliente no tenga que distinguir ausencia de campo y ausencia de valor.
      item.put("reason", rs.getString("reason"));
      return item;
    }, args.toArray());
  }
}
