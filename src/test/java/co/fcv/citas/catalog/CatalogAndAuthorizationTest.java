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

  @Test
  void hu012_ca01_specialtyDurationIsRestrictedToThirtyOrSixty() throws Exception {
    mvc.perform(post("/api/v1/admin/specialties").header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"CAT_45\",\"name\":\"Invalida 45\",\"durationMinutes\":45,"
                + "\"general\":false,\"requiresAdminApproval\":true}"))
        .andExpect(status().isBadRequest());
  }
}
