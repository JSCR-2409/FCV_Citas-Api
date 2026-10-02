package co.fcv.citas.catalog;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * HU-010 y HU-011 — Administracion de EPS y de sus planes.
 *
 * <p>RF-06 prohibe el borrado fisico de un catalogo referenciado por transacciones, de modo que no
 * hay ningun DELETE: la baja es {@code active=false}. Eso conserva el significado de las
 * afiliaciones y citas que ya apuntan a esa EPS o a ese plan, que un borrado dejaria sin referente.
 *
 * <p>La ruta vive bajo {@code /api/v1/admin}, cubierta por la regla {@code hasRole("ADMIN")} de
 * SecurityConfig: eso resuelve CA-03 de ambas HU sin anotacion por metodo.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminInsuranceController {

  private final JdbcTemplate db;

  public AdminInsuranceController(JdbcTemplate db) { this.db = db; }

  // --- EPS: HU-010 --------------------------------------------------------------------------

  /** CA-01. Incluye las desactivadas: son justo las que el ADMIN necesita ver para reactivarlas. */
  @GetMapping("/eps")
  ResponseEntity<?> listEps() {
    return ResponseEntity.ok(db.query(
        "SELECT e.id,e.code,e.name,e.active,"
        + "(SELECT COUNT(*) FROM eps_plans p WHERE p.eps_id=e.id) planCount"
        + " FROM eps e ORDER BY e.name", (rs, n) -> {
          var row = new LinkedHashMap<String, Object>();
          row.put("id", rs.getLong("id"));
          row.put("code", rs.getString("code"));
          row.put("name", rs.getString("name"));
          row.put("active", rs.getBoolean("active"));
          row.put("planCount", rs.getInt("planCount"));
          return row;
        }));
  }

  @PostMapping("/eps")
  @Transactional
  ResponseEntity<?> createEps(@RequestBody EpsInput body) {
    if (body == null || isBlank(body.code()) || isBlank(body.name())) {
      return bad("código y nombre son obligatorios");
    }
    String code = body.code().trim().toUpperCase(java.util.Locale.ROOT);
    try {
      db.update("INSERT INTO eps(code,name,active) VALUES(?,?,TRUE)", code, body.name().trim());
    } catch (DuplicateKeyException e) {
      // El codigo es la clave de negocio: dos EPS con el mismo codigo harian ambiguas las
      // afiliaciones que lo referencian.
      return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "el código de EPS ya existe"));
    }
    long id = db.queryForObject("SELECT id FROM eps WHERE code=?", Long.class, code);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id, "code", code, "name", body.name().trim(), "active", true));
  }

  /** CA-01 y CA-02. No hay DELETE: {@code active:false} es la baja que admite RF-06. */
  @PatchMapping("/eps/{id}")
  @Transactional
  ResponseEntity<?> updateEps(@PathVariable long id, @RequestBody EpsPatch body) {
    if (db.queryForObject("SELECT COUNT(*) FROM eps WHERE id=?", Integer.class, id) == 0) {
      return ResponseEntity.notFound().build();
    }
    if (body == null || (body.name() == null && body.active() == null)) {
      return bad("no hay cambios válidos: se admiten name y active");
    }
    if (body.name() != null && !body.name().isBlank()) {
      db.update("UPDATE eps SET name=? WHERE id=?", body.name().trim(), id);
    }
    if (body.active() != null) {
      db.update("UPDATE eps SET active=? WHERE id=?", body.active(), id);
      // Desactivar la EPS desactiva sus planes: un plan activo de una EPS inactiva seria
      // seleccionable y produciria afiliaciones a una aseguradora que ya no opera.
      if (!body.active()) db.update("UPDATE eps_plans SET active=FALSE WHERE eps_id=?", id);
    }
    return ResponseEntity.ok(Map.of("id", id, "updated", true));
  }

  // --- Planes: HU-011 -----------------------------------------------------------------------

  /** CA-03. Filtra por EPS: los planes de una EPS no deben aparecer bajo otra. */
  @GetMapping("/eps-plans")
  ResponseEntity<?> listPlans(@RequestParam(required = false) Long epsId) {
    var sql = new StringBuilder(
        "SELECT p.id,p.eps_id epsId,e.name epsName,p.regime_id regimeId,r.name regimeName,"
        + "p.code,p.name,p.active FROM eps_plans p"
        + " JOIN eps e ON e.id=p.eps_id"
        + " JOIN insurance_regimes r ON r.id=p.regime_id WHERE 1=1");
    var args = new java.util.ArrayList<Object>();
    if (epsId != null) { sql.append(" AND p.eps_id=?"); args.add(epsId); }
    sql.append(" ORDER BY e.name,p.name");

    return ResponseEntity.ok(db.query(sql.toString(), (rs, n) -> {
      var row = new LinkedHashMap<String, Object>();
      row.put("id", rs.getLong("id"));
      row.put("epsId", rs.getLong("epsId"));
      row.put("epsName", rs.getString("epsName"));
      row.put("regimeId", rs.getLong("regimeId"));
      row.put("regimeName", rs.getString("regimeName"));
      row.put("code", rs.getString("code"));
      row.put("name", rs.getString("name"));
      row.put("active", rs.getBoolean("active"));
      return row;
    }, args.toArray()));
  }

  @PostMapping("/eps-plans")
  @Transactional
  ResponseEntity<?> createPlan(@RequestBody PlanInput body) {
    if (body == null || body.epsId() == null || body.regimeId() == null
        || isBlank(body.code()) || isBlank(body.name())) {
      return bad("EPS, régimen, código y nombre son obligatorios");
    }
    // La EPS debe estar activa: crear un plan bajo una EPS dada de baja lo haria inalcanzable.
    if (db.queryForObject("SELECT COUNT(*) FROM eps WHERE id=? AND active=TRUE",
        Integer.class, body.epsId()) == 0) {
      return bad("la EPS no existe o está inactiva");
    }
    if (db.queryForObject("SELECT COUNT(*) FROM insurance_regimes WHERE id=?",
        Integer.class, body.regimeId()) == 0) {
      return bad("el régimen no existe");
    }
    String code = body.code().trim().toUpperCase(java.util.Locale.ROOT);
    try {
      db.update("INSERT INTO eps_plans(eps_id,regime_id,code,name,active) VALUES(?,?,?,?,TRUE)",
          body.epsId(), body.regimeId(), code, body.name().trim());
    } catch (DuplicateKeyException e) {
      // uq_eps_plan_code es (eps_id, code): el mismo codigo puede repetirse entre EPS distintas.
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("message", "esa EPS ya tiene un plan con ese código"));
    }
    long id = db.queryForObject("SELECT id FROM eps_plans WHERE eps_id=? AND code=?",
        Long.class, body.epsId(), code);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id, "epsId", body.epsId(), "code", code));
  }

  /** CA-01 y CA-02. El plan no cambia de EPS: mover un plan reescribiria afiliaciones existentes. */
  @PatchMapping("/eps-plans/{id}")
  @Transactional
  ResponseEntity<?> updatePlan(@PathVariable long id, @RequestBody PlanPatch body) {
    if (db.queryForObject("SELECT COUNT(*) FROM eps_plans WHERE id=?", Integer.class, id) == 0) {
      return ResponseEntity.notFound().build();
    }
    if (body == null || (body.name() == null && body.active() == null && body.regimeId() == null)) {
      return bad("no hay cambios válidos: se admiten name, regimeId y active");
    }
    if (body.name() != null && !body.name().isBlank()) {
      db.update("UPDATE eps_plans SET name=? WHERE id=?", body.name().trim(), id);
    }
    if (body.regimeId() != null) {
      if (db.queryForObject("SELECT COUNT(*) FROM insurance_regimes WHERE id=?",
          Integer.class, body.regimeId()) == 0) {
        return bad("el régimen no existe");
      }
      db.update("UPDATE eps_plans SET regime_id=? WHERE id=?", body.regimeId(), id);
    }
    if (body.active() != null) {
      if (body.active() && db.queryForObject(
          "SELECT COUNT(*) FROM eps_plans p JOIN eps e ON e.id=p.eps_id WHERE p.id=? AND e.active=TRUE",
          Integer.class, id) == 0) {
        return bad("no se puede reactivar un plan de una EPS inactiva");
      }
      db.update("UPDATE eps_plans SET active=? WHERE id=?", body.active(), id);
    }
    return ResponseEntity.ok(Map.of("id", id, "updated", true));
  }

  private static boolean isBlank(String value) { return value == null || value.isBlank(); }

  private ResponseEntity<Map<String, String>> bad(String message) {
    return ResponseEntity.badRequest().body(Map.of("message", message));
  }

  record EpsInput(String code, String name) {}
  record EpsPatch(String name, Boolean active) {}
  record PlanInput(Long epsId, Long regimeId, String code, String name) {}
  record PlanPatch(String name, Long regimeId, Boolean active) {}
}
