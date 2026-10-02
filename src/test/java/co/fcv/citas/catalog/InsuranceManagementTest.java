package co.fcv.citas.catalog;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.fcv.citas.support.TestData;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HU-010 — Administrar EPS; HU-011 — Administrar planes de EPS; HU-008 — Gestionar afiliacion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InsuranceManagementTest {

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired ObjectMapper json;
  @Autowired jakarta.persistence.EntityManager em;

  TestData data;
  String admin, patient, otherPatient, professional;
  long patientId, otherId, regimeId, otherRegimeId;

  @BeforeEach
  void setUp() throws Exception {
    data = new TestData(mvc, db, json, em);
    admin = data.bearer("ins.admin@test.local", "ADMIN", "INS-1");
    patientId = data.registerUser("ins.patient@test.local", "INS-2");
    patient = "Bearer " + data.login("ins.patient@test.local");
    otherId = data.registerUser("ins.other@test.local", "INS-3");
    otherPatient = "Bearer " + data.login("ins.other@test.local");
    professional = data.bearer("ins.pro@test.local", "PROFESSIONAL", "INS-4");

    var regimes = db.queryForList("SELECT id FROM insurance_regimes ORDER BY id", Long.class);
    regimeId = regimes.get(0);
    otherRegimeId = regimes.size() > 1 ? regimes.get(1) : regimes.get(0);
  }

  private long createEps(String code, String name) throws Exception {
    mvc.perform(post("/api/v1/admin/eps").header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"" + code + "\",\"name\":\"" + name + "\"}"))
        .andExpect(status().isCreated());
    return db.queryForObject("SELECT id FROM eps WHERE code=?", Long.class, code);
  }

  private long createPlan(long epsId, String code, String name) throws Exception {
    mvc.perform(post("/api/v1/admin/eps-plans").header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"epsId\":" + epsId + ",\"regimeId\":" + regimeId
                + ",\"code\":\"" + code + "\",\"name\":\"" + name + "\"}"))
        .andExpect(status().isCreated());
    return db.queryForObject("SELECT id FROM eps_plans WHERE eps_id=? AND code=?", Long.class, epsId, code);
  }

  // --- HU-010 -------------------------------------------------------------------------------

  @Test
  void hu010_ca01_adminCreatesConsultsAndUpdatesAnEps() throws Exception {
    long id = createEps("INS_EPS_A", "EPS Laboratorio A");

    mvc.perform(get("/api/v1/admin/eps").header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.code=='INS_EPS_A')].name").value("EPS Laboratorio A"));

    mvc.perform(patch("/api/v1/admin/eps/" + id).header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"EPS renombrada\"}"))
        .andExpect(status().isOk());
    Assertions.assertEquals("EPS renombrada",
        db.queryForObject("SELECT name FROM eps WHERE id=?", String.class, id));
  }

  @Test
  void hu010_ca01_aDuplicateCodeIsRejected() throws Exception {
    createEps("INS_EPS_DUP", "EPS original");
    mvc.perform(post("/api/v1/admin/eps").header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"INS_EPS_DUP\",\"name\":\"EPS repetida\"}"))
        .andExpect(status().isConflict());
  }

  /**
   * CA-02. RF-06 prohibe el borrado fisico de un catalogo referenciado: la via es la
   * desactivacion, y el ADMIN sigue viendo la EPS inactiva para poder reactivarla.
   */
  @Test
  void hu010_ca02_anEpsIsDeactivatedAndNeverPhysicallyDeleted() throws Exception {
    long id = createEps("INS_EPS_OFF", "EPS a desactivar");
    long planId = createPlan(id, "INS_PLAN_OFF", "Plan de la EPS");
    mvc.perform(put("/api/v1/me/insurance-affiliation").header(HttpHeaders.AUTHORIZATION, patient)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planId\":" + planId + ",\"membershipNumber\":\"AF-1\"}"))
        .andExpect(status().isCreated());

    mvc.perform(patch("/api/v1/admin/eps/" + id).header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
        .andExpect(status().isOk());

    // La fila sigue existiendo y la afiliacion conserva su referente.
    Assertions.assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM eps WHERE id=?", Integer.class, id).intValue());
    Assertions.assertEquals(1, db.queryForObject(
        "SELECT COUNT(*) FROM user_insurance_affiliations WHERE plan_id=?", Integer.class, planId).intValue());

    // El catalogo publico ya no la ofrece, pero el ADMIN si.
    mvc.perform(get("/api/v1/catalogs/eps").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(jsonPath("$[?(@.code=='INS_EPS_OFF')]").isEmpty());
    mvc.perform(get("/api/v1/admin/eps").header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(jsonPath("$[?(@.code=='INS_EPS_OFF')].active").value(false));
  }

  /** Un plan activo de una EPS inactiva seria seleccionable y produciria una afiliacion invalida. */
  @Test
  void hu010_ca02_deactivatingAnEpsAlsoDeactivatesItsPlans() throws Exception {
    long id = createEps("INS_EPS_CASC", "EPS en cascada");
    long planId = createPlan(id, "INS_PLAN_CASC", "Plan en cascada");

    mvc.perform(patch("/api/v1/admin/eps/" + id).header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
        .andExpect(status().isOk());

    Assertions.assertFalse(db.queryForObject("SELECT active FROM eps_plans WHERE id=?", Boolean.class, planId));
  }

  @Test
  void hu010_ca03_onlyAdminCanManageEps() throws Exception {
    for (String token : new String[] {patient, professional}) {
      mvc.perform(get("/api/v1/admin/eps").header(HttpHeaders.AUTHORIZATION, token))
          .andExpect(status().isForbidden());
      mvc.perform(post("/api/v1/admin/eps").header(HttpHeaders.AUTHORIZATION, token)
              .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"X\",\"name\":\"X\"}"))
          .andExpect(status().isForbidden());
    }
  }

  // --- HU-011 -------------------------------------------------------------------------------

  @Test
  void hu011_ca01_aPlanBelongsToTheEpsItWasCreatedUnder() throws Exception {
    long epsId = createEps("INS_EPS_P", "EPS con planes");
    long planId = createPlan(epsId, "INS_PLAN_P", "Plan basico");

    Assertions.assertEquals(epsId,
        db.queryForObject("SELECT eps_id FROM eps_plans WHERE id=?", Long.class, planId).longValue());

    mvc.perform(patch("/api/v1/admin/eps-plans/" + planId).header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Plan renombrado\",\"regimeId\":" + otherRegimeId + "}"))
        .andExpect(status().isOk());
    Assertions.assertEquals("Plan renombrado",
        db.queryForObject("SELECT name FROM eps_plans WHERE id=?", String.class, planId));
    // Renombrar o cambiar el regimen no mueve el plan de EPS.
    Assertions.assertEquals(epsId,
        db.queryForObject("SELECT eps_id FROM eps_plans WHERE id=?", Long.class, planId).longValue());
  }

  @Test
  void hu011_ca01_aPlanCannotBeCreatedUnderAnInactiveEps() throws Exception {
    long epsId = createEps("INS_EPS_INACT", "EPS inactiva");
    mvc.perform(patch("/api/v1/admin/eps/" + epsId).header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
        .andExpect(status().isOk());

    mvc.perform(post("/api/v1/admin/eps-plans").header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"epsId\":" + epsId + ",\"regimeId\":" + regimeId
                + ",\"code\":\"INS_PLAN_X\",\"name\":\"Plan imposible\"}"))
        .andExpect(status().isBadRequest());
  }

  /** uq_eps_plan_code es (eps_id, code): el mismo codigo puede repetirse entre EPS distintas. */
  @Test
  void hu011_ca01_thePlanCodeIsUniqueWithinItsEpsOnly() throws Exception {
    long first = createEps("INS_EPS_U1", "EPS uno");
    long second = createEps("INS_EPS_U2", "EPS dos");
    createPlan(first, "PLAN_COMUN", "Plan en EPS uno");

    mvc.perform(post("/api/v1/admin/eps-plans").header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"epsId\":" + first + ",\"regimeId\":" + regimeId
                + ",\"code\":\"PLAN_COMUN\",\"name\":\"Repetido\"}"))
        .andExpect(status().isConflict());

    createPlan(second, "PLAN_COMUN", "Plan en EPS dos");
  }

  @Test
  void hu011_ca02_aPlanIsDeactivatedNotDeleted() throws Exception {
    long epsId = createEps("INS_EPS_PD", "EPS plan baja");
    long planId = createPlan(epsId, "INS_PLAN_PD", "Plan a desactivar");

    mvc.perform(patch("/api/v1/admin/eps-plans/" + planId).header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
        .andExpect(status().isOk());

    Assertions.assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM eps_plans WHERE id=?", Integer.class, planId).intValue());
    mvc.perform(get("/api/v1/catalogs/eps/" + epsId + "/plans").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(jsonPath("$.length()").value(0));
  }

  /** Reactivar un plan cuya EPS sigue inactiva lo haria seleccionable sin aseguradora operativa. */
  @Test
  void hu011_ca02_aPlanCannotBeReactivatedWhileItsEpsIsInactive() throws Exception {
    long epsId = createEps("INS_EPS_RE", "EPS reactivacion");
    long planId = createPlan(epsId, "INS_PLAN_RE", "Plan reactivable");
    mvc.perform(patch("/api/v1/admin/eps/" + epsId).header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
        .andExpect(status().isOk());

    mvc.perform(patch("/api/v1/admin/eps-plans/" + planId).header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void hu011_ca03_thePlansOfOneEpsNeverAppearUnderAnother() throws Exception {
    long first = createEps("INS_EPS_C1", "EPS coherente uno");
    long second = createEps("INS_EPS_C2", "EPS coherente dos");
    createPlan(first, "INS_PLAN_C1", "Plan de la uno");
    createPlan(second, "INS_PLAN_C2", "Plan de la dos");

    mvc.perform(get("/api/v1/catalogs/eps/" + first + "/plans").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].code").value("INS_PLAN_C1"));

    mvc.perform(get("/api/v1/admin/eps-plans?epsId=" + second).header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].code").value("INS_PLAN_C2"));
  }

  // --- HU-008 -------------------------------------------------------------------------------

  @Test
  void hu008_ca01_theAffiliationRelatesEpsPlanAndRegimeWithoutCopyingNames() throws Exception {
    long epsId = createEps("INS_EPS_AF", "EPS afiliacion");
    long planId = createPlan(epsId, "INS_PLAN_AF", "Plan afiliacion");

    mvc.perform(put("/api/v1/me/insurance-affiliation").header(HttpHeaders.AUTHORIZATION, patient)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planId\":" + planId + ",\"membershipNumber\":\"AF-1001\"}"))
        .andExpect(status().isCreated());

    // Solo plan_id se persiste: la EPS y el regimen se derivan por JOIN, segun el requisito 3FN.
    var columns = db.queryForList("SELECT * FROM user_insurance_affiliations WHERE user_id=?", patientId).get(0);
    Assertions.assertFalse(columns.keySet().stream().anyMatch(k -> k.toLowerCase().contains("eps_name")),
        "la afiliación no debe copiar nombres del catálogo");

    mvc.perform(get("/api/v1/me/insurance-affiliation").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.affiliation.epsName").value("EPS afiliacion"))
        .andExpect(jsonPath("$.affiliation.planName").value("Plan afiliacion"))
        .andExpect(jsonPath("$.affiliation.regimeName").exists())
        .andExpect(jsonPath("$.affiliation.membershipNumber").value("AF-1001"));
  }

  /** CA-01: renombrar la EPS debe reflejarse en la afiliacion, porque no se copio el nombre. */
  @Test
  void hu008_ca01_renamingTheEpsIsVisibleInTheExistingAffiliation() throws Exception {
    long epsId = createEps("INS_EPS_RN", "Nombre viejo");
    long planId = createPlan(epsId, "INS_PLAN_RN", "Plan");
    mvc.perform(put("/api/v1/me/insurance-affiliation").header(HttpHeaders.AUTHORIZATION, patient)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planId\":" + planId + ",\"membershipNumber\":\"AF-2\"}"))
        .andExpect(status().isCreated());

    mvc.perform(patch("/api/v1/admin/eps/" + epsId).header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Nombre nuevo\"}"))
        .andExpect(status().isOk());

    mvc.perform(get("/api/v1/me/insurance-affiliation").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(jsonPath("$.affiliation.epsName").value("Nombre nuevo"));
  }

  @Test
  void hu008_ca02_aPlanThatDoesNotBelongToTheGivenEpsIsRejected() throws Exception {
    long first = createEps("INS_EPS_M1", "EPS mezcla uno");
    long second = createEps("INS_EPS_M2", "EPS mezcla dos");
    long planOfFirst = createPlan(first, "INS_PLAN_M1", "Plan de la uno");

    mvc.perform(put("/api/v1/me/insurance-affiliation").header(HttpHeaders.AUTHORIZATION, patient)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planId\":" + planOfFirst + ",\"epsId\":" + second + ",\"membershipNumber\":\"AF-3\"}"))
        .andExpect(status().isBadRequest());

    Assertions.assertEquals(0, db.queryForObject(
        "SELECT COUNT(*) FROM user_insurance_affiliations WHERE user_id=?", Integer.class, patientId).intValue());
  }

  @Test
  void hu008_ca02_anInactivePlanCannotBeAffiliated() throws Exception {
    long epsId = createEps("INS_EPS_IP", "EPS plan inactivo");
    long planId = createPlan(epsId, "INS_PLAN_IP", "Plan inactivo");
    mvc.perform(patch("/api/v1/admin/eps-plans/" + planId).header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
        .andExpect(status().isOk());

    mvc.perform(put("/api/v1/me/insurance-affiliation").header(HttpHeaders.AUTHORIZATION, patient)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planId\":" + planId + ",\"membershipNumber\":\"AF-4\"}"))
        .andExpect(status().isBadRequest());
  }

  /** RF-04: no duplicar EPS, regimen y plan dentro del usuario. Solo una afiliacion vigente. */
  @Test
  void hu008_ca01_replacingTheAffiliationLeavesExactlyOneCurrent() throws Exception {
    long epsId = createEps("INS_EPS_SW", "EPS cambio");
    long planA = createPlan(epsId, "INS_PLAN_SW_A", "Plan A");
    long planB = createPlan(epsId, "INS_PLAN_SW_B", "Plan B");

    mvc.perform(put("/api/v1/me/insurance-affiliation").header(HttpHeaders.AUTHORIZATION, patient)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planId\":" + planA + ",\"membershipNumber\":\"AF-5\"}"))
        .andExpect(status().isCreated());
    mvc.perform(put("/api/v1/me/insurance-affiliation").header(HttpHeaders.AUTHORIZATION, patient)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planId\":" + planB + ",\"membershipNumber\":\"AF-6\"}"))
        .andExpect(status().isCreated());

    Assertions.assertEquals(1, db.queryForObject(
        "SELECT COUNT(*) FROM user_insurance_affiliations WHERE user_id=? AND is_current=TRUE",
        Integer.class, patientId).intValue());
    mvc.perform(get("/api/v1/me/insurance-affiliation").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(jsonPath("$.affiliation.planId").value(planB));
  }

  /** Volver a una afiliacion anterior no puede violar uq_user_membership. */
  @Test
  void hu008_ca01_goingBackToAPreviousAffiliationReactivatesIt() throws Exception {
    long epsId = createEps("INS_EPS_BK", "EPS vuelta");
    long planA = createPlan(epsId, "INS_PLAN_BK_A", "Plan A");
    long planB = createPlan(epsId, "INS_PLAN_BK_B", "Plan B");

    for (long plan : new long[] {planA, planB}) {
      mvc.perform(put("/api/v1/me/insurance-affiliation").header(HttpHeaders.AUTHORIZATION, patient)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"planId\":" + plan + ",\"membershipNumber\":\"AF-VUELTA\"}"))
          .andExpect(status().isCreated());
    }
    mvc.perform(put("/api/v1/me/insurance-affiliation").header(HttpHeaders.AUTHORIZATION, patient)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planId\":" + planA + ",\"membershipNumber\":\"AF-VUELTA\"}"))
        .andExpect(status().isOk());

    Assertions.assertEquals(1, db.queryForObject(
        "SELECT COUNT(*) FROM user_insurance_affiliations WHERE user_id=? AND is_current=TRUE",
        Integer.class, patientId).intValue());
  }

  @Test
  void hu008_ca03_aUserOnlyOperatesTheirOwnAffiliation() throws Exception {
    long epsId = createEps("INS_EPS_OW", "EPS ownership");
    long planId = createPlan(epsId, "INS_PLAN_OW", "Plan ownership");
    mvc.perform(put("/api/v1/me/insurance-affiliation").header(HttpHeaders.AUTHORIZATION, patient)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planId\":" + planId + ",\"membershipNumber\":\"AF-7\"}"))
        .andExpect(status().isCreated());

    // El otro usuario no ve la afiliacion ajena y la suya sigue vacia.
    mvc.perform(get("/api/v1/me/insurance-affiliation").header(HttpHeaders.AUTHORIZATION, otherPatient))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.affiliation.planId").doesNotExist());
    Assertions.assertEquals(0, db.queryForObject(
        "SELECT COUNT(*) FROM user_insurance_affiliations WHERE user_id=?", Integer.class, otherId).intValue());
  }

  @Test
  void hu008_ca03_anonymousCallersCannotReachTheAffiliation() throws Exception {
    mvc.perform(get("/api/v1/me/insurance-affiliation")).andExpect(status().isForbidden());
  }
}
