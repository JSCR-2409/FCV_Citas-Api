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
 * HU-009 — Consultar catalogos fijos y la autorizacion por rol que exigen HU-012, HU-013,
 * HU-014 y HU-015: ningun actor distinto de ADMIN puede operar los endpoints administrativos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogAndAuthorizationTest {

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired ObjectMapper json;
  @Autowired jakarta.persistence.EntityManager em;

  TestData data;
  String patient, professional, admin;

  @BeforeEach
  void setUp() throws Exception {
    data = new TestData(mvc, db, json, em);
    patient = data.bearer("cat.patient@test.local", "USER", "CAT-1");
    professional = data.bearer("cat.pro@test.local", "PROFESSIONAL", "CAT-2");
    admin = data.bearer("cat.admin@test.local", "ADMIN", "CAT-3");
  }

  @Test
  void ca01_fixedCatalogsAreAvailable() throws Exception {
    for (String path : new String[] {"roles", "locations", "appointment-statuses", "reschedule-statuses", "regimes"}) {
      mvc.perform(get("/api/v1/catalogs/" + path).header(HttpHeaders.AUTHORIZATION, patient))
          .andExpect(status().isOk());
    }
  }

  @Test
  void ca01_locationsReturnTheTwoFixedSites() throws Exception {
    mvc.perform(get("/api/v1/catalogs/locations").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void ca01_appointmentStatusesCoverTheWholeLifecycle() throws Exception {
    String body = mvc.perform(get("/api/v1/catalogs/appointment-statuses").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    for (String code : new String[] {"REQUESTED", "APPROVED", "REJECTED", "CANCELLED", "COMPLETED", "NO_SHOW"}) {
      Assertions.assertTrue(body.contains("\"" + code + "\""), "falta el estado " + code);
    }
  }

  @Test
  void ca02_catalogsAreNotWritable() throws Exception {
    mvc.perform(post("/api/v1/catalogs/locations").header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"X\"}"))
        .andExpect(result -> Assertions.assertNotEquals(201, result.getResponse().getStatus(),
            "los catalogos fijos no deben admitir escritura"));
  }

  @Test
  void adminEndpointsRejectPatientAndProfessional() throws Exception {
    String[][] calls = {
        {"POST", "/api/v1/admin/specialties"},
        {"POST", "/api/v1/admin/professionals"},
        {"GET", "/api/v1/admin/specialized-requests"},
    };
    for (String token : new String[] {patient, professional}) {
      for (String[] call : calls) {
        var request = "POST".equals(call[0])
            ? post(call[1]).contentType(MediaType.APPLICATION_JSON).content("{}")
            : get(call[1]);
        mvc.perform(request.header(HttpHeaders.AUTHORIZATION, token))
            .andExpect(status().isForbidden());
      }
    }
  }

  /**
   * El contrato catalogos-fijos-rest.md fija explicitamente que sin autenticacion la respuesta
   * es 403 "segun la configuracion actual de Spring Security", asi que es el comportamiento
   * aprobado y es lo que se verifica aqui.
   * PREGUNTA ABIERTA: para una API con JWT lo habitual seria 401 para no autenticado y 403 para
   * autenticado sin permiso. Cambiarlo requiere revisar el contrato, no inferirlo desde el codigo.
   */
  @Test
  void anonymousCallersAreRejectedAsTheContractStates() throws Exception {
    mvc.perform(get("/api/v1/admin/specialized-requests")).andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/availability?date=2030-01-01&specialtyId=1")).andExpect(status().isForbidden());
  }

  /**
   * user_roles es N:M y el PRD admite usuarios con varios roles: el token debe llevarlos todos
   * y conceder una autoridad por cada uno, no solo la del rol alfabeticamente primero.
   */
  @Test
  void aUserWithSeveralRolesGetsAllOfThem() throws Exception {
    long id = data.registerUser("cat.multi@test.local", "CAT-4");
    // El flush va primero: el alta via API deja el rol original pendiente en la sesion JPA y,
    // si se volcara despues, reinsertaria la fila que se acaba de escribir aqui.
    em.flush();
    db.update("DELETE FROM user_roles WHERE user_id=?", id);
    for (String code : new String[] {"USER", "PROFESSIONAL", "ADMIN"}) {
      db.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,r.id FROM roles r WHERE r.code=?", id, code);
    }
    em.clear();
    String token = "Bearer " + data.login("cat.multi@test.local");

    // Endpoint exclusivo de ADMIN y endpoint de cualquier autenticado, con el mismo token.
    mvc.perform(get("/api/v1/admin/specialized-requests").header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles.length()").value(3))
        .andExpect(jsonPath("$.role").value("ADMIN"));
  }

  @Test
  void aUserWithASingleRoleDoesNotGainOthers() throws Exception {
    mvc.perform(get("/api/v1/admin/specialized-requests").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles.length()").value(1))
        .andExpect(jsonPath("$.role").value("USER"));
  }

  @Test
  void hu012_ca01_specialtyDurationIsRestrictedToThirtyOrSixty() throws Exception {
    mvc.perform(post("/api/v1/admin/specialties").header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"CAT_45\",\"name\":\"Invalida 45\",\"durationMinutes\":45,"
                + "\"general\":false,\"requiresAdminApproval\":true}"))
        .andExpect(status().isBadRequest());
  }

  /** CA-01 dice "crea o actualiza": la restriccion aplica tambien al PATCH, no solo al alta. */
  @Test
  void hu012_ca01_updatingToAnInvalidDurationIsRejected() throws Exception {
    String created = mvc.perform(post("/api/v1/admin/specialties").header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"CAT_UPD_30\",\"name\":\"Actualizable 30\",\"durationMinutes\":30,"
                + "\"general\":false,\"requiresAdminApproval\":true}"))
        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    Assertions.assertTrue(created.contains("CAT_UPD_30"));
    long id = db.queryForObject("SELECT id FROM specialties WHERE code=?", Long.class, "CAT_UPD_30");

    mvc.perform(patch("/api/v1/admin/specialties/" + id).header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"durationMinutes\":45}"))
        .andExpect(status().isBadRequest());
    Assertions.assertEquals(30, db.queryForObject(
        "SELECT appointment_duration_minutes FROM specialties WHERE id=?", Integer.class, id).intValue());

    mvc.perform(patch("/api/v1/admin/specialties/" + id).header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"durationMinutes\":60}"))
        .andExpect(status().isOk());
    Assertions.assertEquals(60, db.queryForObject(
        "SELECT appointment_duration_minutes FROM specialties WHERE id=?", Integer.class, id).intValue());
  }

  /** HU-012 CA-02: una especialidad referenciada se desactiva, no se borra, y el ADMIN la ve. */
  @Test
  void hu012_ca02_adminSeesInactiveSpecialtiesToReactivateThem() throws Exception {
    mvc.perform(post("/api/v1/admin/specialties").header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"CAT_OFF_30\",\"name\":\"Desactivable 30\",\"durationMinutes\":30,"
                + "\"general\":false,\"requiresAdminApproval\":true}"))
        .andExpect(status().isCreated());
    long id = db.queryForObject("SELECT id FROM specialties WHERE code=?", Long.class, "CAT_OFF_30");

    mvc.perform(patch("/api/v1/admin/specialties/" + id).header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
        .andExpect(status().isOk());

    // El catalogo publico ya no la ofrece...
    String publicCatalog = mvc.perform(get("/api/v1/catalogs/specialties")
            .header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    Assertions.assertFalse(publicCatalog.contains("CAT_OFF_30"));

    // ...pero el ADMIN si, para poder reactivarla.
    String adminCatalog = mvc.perform(get("/api/v1/admin/specialties").header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    Assertions.assertTrue(adminCatalog.contains("CAT_OFF_30"));
  }

  @Test
  void hu012_ca02_onlyAdminCanListEverySpecialty() throws Exception {
    mvc.perform(get("/api/v1/admin/specialties").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isForbidden());
  }
}
