package co.fcv.citas.user;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * HU-008 — Afiliacion del usuario a una EPS, plan y regimen.
 *
 * <p>CA-01 pide relacionar "sin copiar sus nombres": la afiliacion guarda solo {@code plan_id}, y
 * la EPS y el regimen se derivan del plan por JOIN. Duplicar los nombres los dejaria congelados y
 * contradiria el requisito 3FN de no repetir datos del catalogo en la transaccion.
 *
 * <p>El plan ya determina la EPS, de modo que el cliente envia unicamente {@code planId}: pedir
 * tambien el {@code epsId} crearia la posibilidad de una combinacion incoherente, que es
 * exactamente lo que CA-02 prohibe. La verificacion de coherencia se mantiene para el caso en que
 * el cliente envie ambos.
 */
@RestController
@RequestMapping("/api/v1/me/insurance-affiliation")
public class InsuranceAffiliationController {

  private final JdbcTemplate db;

  public InsuranceAffiliationController(JdbcTemplate db) { this.db = db; }

  /** CA-03. El usuario sale del token, de modo que solo puede leer la propia. */
  @GetMapping
  ResponseEntity<?> current(Authentication auth) {
    var rows = db.query(
        "SELECT a.id,a.membership_number membershipNumber,a.is_current isCurrent,"
        + "p.id planId,p.name planName,e.id epsId,e.name epsName,r.id regimeId,r.name regimeName,"
        + "p.active planActive,e.active epsActive"
        + " FROM user_insurance_affiliations a"
        + " JOIN eps_plans p ON p.id=a.plan_id"
        + " JOIN eps e ON e.id=p.eps_id"
        + " JOIN insurance_regimes r ON r.id=p.regime_id"
        + " WHERE a.user_id=? AND a.is_current=TRUE", (rs, n) -> {
          var row = new LinkedHashMap<String, Object>();
          row.put("id", rs.getLong("id"));
          row.put("membershipNumber", rs.getString("membershipNumber"));
          row.put("planId", rs.getLong("planId"));
          row.put("planName", rs.getString("planName"));
          row.put("epsId", rs.getLong("epsId"));
          row.put("epsName", rs.getString("epsName"));
          row.put("regimeId", rs.getLong("regimeId"));
          row.put("regimeName", rs.getString("regimeName"));
          // Una afiliacion vigente a un plan dado de baja sigue siendo la del usuario, pero la UI
          // debe poder avisarle de que necesita actualizarla.
          row.put("catalogActive", rs.getBoolean("planActive") && rs.getBoolean("epsActive"));
          return row;
        }, Long.parseLong(auth.getName()));

    // Sin afiliacion la respuesta es 200 con null y no 404: no tener EPS registrada es un estado
    // normal del perfil, no un error de la peticion.
    return ResponseEntity.ok(rows.isEmpty() ? Map.of("affiliation", java.util.Optional.empty())
        : Map.of("affiliation", rows.get(0)));
  }

  /**
   * CA-01 y CA-02. Reemplaza la afiliacion vigente. RF-04 exige "evitar duplicar EPS, regimen y
   * plan dentro del usuario": en lugar de acumular filas activas, la anterior se marca como no
   * vigente y queda como historico.
   */
  @PutMapping
  @Transactional
  ResponseEntity<?> save(@RequestBody Affiliation body, Authentication auth) {
    long userId = Long.parseLong(auth.getName());
    if (body == null || body.planId() == null || body.membershipNumber() == null
        || body.membershipNumber().isBlank()) {
      return bad("plan y número de afiliación son obligatorios");
    }

    var plans = db.query("SELECT p.eps_id FROM eps_plans p JOIN eps e ON e.id=p.eps_id"
        + " WHERE p.id=? AND p.active=TRUE AND e.active=TRUE",
        (rs, n) -> rs.getLong(1), body.planId());
    if (plans.isEmpty()) return bad("el plan no existe o no está activo");

    // CA-02. El plan ya determina su EPS; si el cliente envia una distinta, la peticion es
    // incoherente y se rechaza en lugar de elegir cual de las dos vale.
    if (body.epsId() != null && !body.epsId().equals(plans.get(0))) {
      return bad("el plan no pertenece a la EPS indicada");
    }

    db.update("UPDATE user_insurance_affiliations SET is_current=FALSE WHERE user_id=? AND is_current=TRUE", userId);
    String membership = body.membershipNumber().trim();
    // uq_user_membership es (user_id, plan_id, membership_number): si el usuario vuelve a una
    // afiliacion que ya tuvo, se reactiva la fila existente en lugar de violar la restriccion.
    int reactivated = db.update(
        "UPDATE user_insurance_affiliations SET is_current=TRUE WHERE user_id=? AND plan_id=? AND membership_number=?",
        userId, body.planId(), membership);
    if (reactivated == 0) {
      db.update("INSERT INTO user_insurance_affiliations(user_id,plan_id,membership_number,is_current)"
          + " VALUES(?,?,?,TRUE)", userId, body.planId(), membership);
    }
    return ResponseEntity.status(reactivated == 0 ? HttpStatus.CREATED : HttpStatus.OK)
        .body(Map.of("planId", body.planId(), "membershipNumber", membership, "isCurrent", true));
  }

  private ResponseEntity<Map<String, String>> bad(String message) {
    return ResponseEntity.badRequest().body(Map.of("message", message));
  }

  record Affiliation(Long planId, Long epsId, String membershipNumber) {}
}
