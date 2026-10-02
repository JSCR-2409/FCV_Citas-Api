package co.fcv.citas.catalog;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalogs")
public class CatalogController {
  private final JdbcTemplate db;
  public CatalogController(JdbcTemplate db) { this.db = db; }
  @GetMapping("/roles") List<?> roles() { return db.query("SELECT id,code,name FROM roles ORDER BY id", (r,n)->new Item(r.getLong("id"),r.getString("code"),r.getString("name"))); }
  @GetMapping("/locations") List<?> locations() { return db.query("SELECT id,code,name,address,city,department FROM locations WHERE active=TRUE ORDER BY name", (r,n) -> new Location(r.getLong("id"),r.getString("code"),r.getString("name"),r.getString("address"),r.getString("city"),r.getString("department"))); }
  @GetMapping("/specialties") List<?> specialties() { return db.query("SELECT id,code,name,appointment_duration_minutes,is_general,requires_admin_approval,active FROM specialties WHERE active=TRUE ORDER BY name", (r,n) -> specialty(r)); }
  @GetMapping("/appointment-statuses") List<?> appointmentStatuses() { return db.query("SELECT id,code,name FROM appointment_statuses ORDER BY id", (r,n)->new Item(r.getLong("id"),r.getString("code"),r.getString("name"))); }
  @GetMapping("/reschedule-statuses") List<?> rescheduleStatuses() { return db.query("SELECT id,code,name FROM reschedule_request_statuses ORDER BY id", (r,n)->new Item(r.getLong("id"),r.getString("code"),r.getString("name"))); }
  @GetMapping("/regimes") List<?> regimes() { return db.query("SELECT id,code,name FROM insurance_regimes ORDER BY name", (r,n)->new Item(r.getLong("id"),r.getString("code"),r.getString("name"))); }
  static Specialty specialty(java.sql.ResultSet r) throws java.sql.SQLException { return new Specialty(r.getLong("id"),r.getString("code"),r.getString("name"),r.getInt("appointment_duration_minutes"),r.getBoolean("is_general"),r.getBoolean("requires_admin_approval"),r.getBoolean("active")); }
  record Item(Long id,String code,String name) {}
  record Location(Long id,String code,String name,String address,String city,String department) {}
  record Specialty(Long id,String code,String name,int durationMinutes,boolean general,boolean requiresAdminApproval,boolean active) {}
}
