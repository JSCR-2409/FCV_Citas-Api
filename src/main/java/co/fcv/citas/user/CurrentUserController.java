package co.fcv.citas.user;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class CurrentUserController {
  private final JdbcTemplate db;
  public CurrentUserController(JdbcTemplate db) { this.db = db; }

  @GetMapping
  Map<String,Object> profile(Authentication auth) {
    return db.queryForMap("SELECT u.id,u.first_name names,u.last_name surnames,u.document_type documentType,u.document_number documentNumber,u.email,u.phone,u.active,r.code role FROM users u LEFT JOIN user_roles ur ON ur.user_id=u.id LEFT JOIN roles r ON r.id=ur.role_id WHERE u.id=? ORDER BY r.id LIMIT 1", Long.parseLong(auth.getName()));
  }

  @GetMapping("/appointments")
  Object appointments(Authentication auth) {
    return db.query("SELECT a.id,a.scheduled_start_at startAt,a.scheduled_end_at endAt,a.status_id statusId,s.code status,p.first_name names,p.last_name surnames,sp.name specialty,l.code locationCode,l.name locationName FROM appointments a JOIN appointment_statuses s ON s.id=a.status_id JOIN professionals pr ON pr.id=a.professional_id JOIN users p ON p.id=pr.user_id JOIN specialties sp ON sp.id=a.specialty_id JOIN locations l ON l.id=a.location_id WHERE a.patient_user_id=? ORDER BY a.scheduled_start_at DESC", (rs,n)->Map.of("id",rs.getLong("id"),"startAt",rs.getTimestamp("startAt").toLocalDateTime(),"endAt",rs.getTimestamp("endAt").toLocalDateTime(),"status",rs.getString("status"),"doctorName",rs.getString("names")+" "+rs.getString("surnames"),"specialty",rs.getString("specialty"),"facility",rs.getString("locationCode"),"facilityFullName",rs.getString("locationName")), Long.parseLong(auth.getName()));
  }
}
