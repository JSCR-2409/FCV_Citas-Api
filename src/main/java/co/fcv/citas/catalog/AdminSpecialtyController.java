package co.fcv.citas.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/specialties")
public class AdminSpecialtyController {
  private final JdbcTemplate db;
  public AdminSpecialtyController(JdbcTemplate db) { this.db = db; }
  @PostMapping ResponseEntity<?> create(@Valid @RequestBody Request r) {
    if (r.durationMinutes()!=30 && r.durationMinutes()!=60) return ResponseEntity.badRequest().body(Map.of("message","duración debe ser 30 o 60"));
    try { db.update("INSERT INTO specialties(code,name,appointment_duration_minutes,is_general,requires_admin_approval,active) VALUES(?,?,?,?,?,TRUE)",r.code(),r.name(),r.durationMinutes(),r.general(),r.requiresAdminApproval()); return ResponseEntity.status(HttpStatus.CREATED).body(r); }
    catch (org.springframework.dao.DuplicateKeyException e) { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message","especialidad duplicada")); }
  }
  /**
   * Listado completo, incluidas las inactivas: el catalogo publico solo devuelve las activas y el
   * ADMIN necesita ver las desactivadas para poder reactivarlas.
   */
  @GetMapping ResponseEntity<?> list() {
    return ResponseEntity.ok(db.query("SELECT id,code,name,appointment_duration_minutes,is_general,"
        + "requires_admin_approval,active FROM specialties ORDER BY name",
        (r,n) -> {
          var item = new java.util.LinkedHashMap<String,Object>();
          item.put("id", r.getLong(1));
          item.put("code", r.getString(2));
          item.put("name", r.getString(3));
          item.put("durationMinutes", r.getInt(4));
          item.put("general", r.getBoolean(5));
          item.put("requiresAdminApproval", r.getBoolean(6));
          item.put("active", r.getBoolean(7));
          return item;
        }));
  }

  @PatchMapping("/{id}") ResponseEntity<?> update(@PathVariable long id,@RequestBody Update r) {
    // HU-012 CA-01 cubre crear "o actualizar": la duracion solo admite 30 o 60 tambien aqui.
    if (r.durationMinutes()!=null && r.durationMinutes()!=30 && r.durationMinutes()!=60) return ResponseEntity.badRequest().body(Map.of("message","duración debe ser 30 o 60"));
    int changed=db.update("UPDATE specialties SET name=COALESCE(?,name), appointment_duration_minutes=COALESCE(?,appointment_duration_minutes), active=COALESCE(?,active) WHERE id=?",r.name(),r.durationMinutes(),r.active(),id);
    return changed==0?ResponseEntity.notFound().build():ResponseEntity.ok(Map.of("id",id,"updated",true));
  }
  record Request(@NotBlank String code,@NotBlank String name,@NotNull Integer durationMinutes,boolean general,boolean requiresAdminApproval) {}
  record Update(String name,Integer durationMinutes,Boolean active) {}
}
