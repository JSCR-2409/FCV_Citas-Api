package co.fcv.citas.professional;

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
 * HU-013 crear profesional, HU-014 asignar especialidades y HU-015 asignar sedes y estado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfessionalManagementTest {

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired ObjectMapper json;
  @Autowired jakarta.persistence.EntityManager em;

  TestData data;
  String admin, patient;
  long hic, icv, activeSpecialty, otherSpecialty, inactiveSpecialty, professional;

  @BeforeEach
  void setUp() throws Exception {
    data = new TestData(mvc, db, json, em);
    hic = data.locationId("HIC");
    icv = data.locationId("ICV");

    activeSpecialty = data.createSpecialty("PM_ACT_30", "Gestion activa 30", 30, false, true);
    otherSpecialty = data.createSpecialty("PM_OTRA_60", "Gestion otra 60", 60, false, true);
    inactiveSpecialty = data.createSpecialty("PM_INACT_30", "Gestion inactiva 30", 30, false, false);

    admin = data.bearer("pm.admin@test.local", "ADMIN", "PM-1");
    patient = data.bearer("pm.patient@test.local", "USER", "PM-2");

    long professionalUser = data.registerUser("pm.pro@test.local", "PM-3");
    data.setRole(professionalUser, "PROFESSIONAL");
    professional = data.createProfessional(professionalUser, "PM-PROF-1");
    data.assignLocation(professional, hic);
  }

  private org.springframework.test.web.servlet.ResultActions adminCall(String method, String path, String body)
      throws Exception {
    var request = switch (method) {
      case "POST" -> post(path);
      case "PUT" -> put(path);
      default -> patch(path);
    };
    return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, admin)
        .contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private String creation(String email, String document, String code) {
    return "{\"names\":\"Nuevo\",\"surnames\":\"Profesional\",\"documentType\":\"CC\","
        + "\"documentNumber\":\"" + document + "\",\"email\":\"" + email + "\",\"phone\":\"3001112233\","
        + "\"temporaryPassword\":\"ClaveTemporal123\"," // allow-secret: dato sintetico de pruebas
        + "\"professionalCode\":\"" + code + "\",\"licenseNumber\":\"LIC-" + code + "\"}";
  }

  @Test
  void hu013_ca01_adminCreatesProfessionalWithItsRole() throws Exception {
    adminCall("POST", "/api/v1/admin/professionals", creation("pm.new@test.local", "PM-10", "PM-PROF-NEW"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.professionalCode").value("PM-PROF-NEW"));

    Long userId = db.queryForObject("SELECT id FROM users WHERE email=?", Long.class, "pm.new@test.local");
    Assertions.assertEquals(1, db.queryForObject(
        "SELECT COUNT(*) FROM user_roles ur JOIN roles r ON r.id=ur.role_id"
        + " WHERE ur.user_id=? AND r.code='PROFESSIONAL'", Integer.class, userId).intValue());
    Assertions.assertEquals(1, db.queryForObject(
        "SELECT COUNT(*) FROM professionals WHERE user_id=?", Integer.class, userId).intValue());
  }

  @Test
  void hu013_ca01_passwordIsNeverReturnedNorStoredInClear() throws Exception {
    String body = adminCall("POST", "/api/v1/admin/professionals",
        creation("pm.hash@test.local", "PM-11", "PM-PROF-HASH"))
        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

    Assertions.assertFalse(body.contains("ClaveTemporal123"), "la respuesta no debe exponer la contrasena");
    String hash = db.queryForObject("SELECT password_hash FROM users WHERE email=?", String.class,
        "pm.hash@test.local");
    Assertions.assertNotEquals("ClaveTemporal123", hash);
    Assertions.assertTrue(hash != null && hash.startsWith("$2"), "debe quedar un hash BCrypt");
  }

  @Test
  void hu013_ca02_onlyAdminCanCreateProfessionals() throws Exception {
    mvc.perform(post("/api/v1/admin/professionals").header(HttpHeaders.AUTHORIZATION, patient)
            .contentType(MediaType.APPLICATION_JSON).content(creation("pm.x@test.local", "PM-12", "PM-PROF-X")))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/admin/professionals")
            .contentType(MediaType.APPLICATION_JSON).content(creation("pm.y@test.local", "PM-13", "PM-PROF-Y")))
        .andExpect(status().isForbidden());
    Assertions.assertEquals(0, db.queryForObject(
        "SELECT COUNT(*) FROM users WHERE email IN ('pm.x@test.local','pm.y@test.local')",
        Integer.class).intValue());
  }

  @Test
  void hu013_ca02_duplicatedIdentifiersAreRejected() throws Exception {
    adminCall("POST", "/api/v1/admin/professionals", creation("pm.dup@test.local", "PM-14", "PM-PROF-DUP"))
        .andExpect(status().isCreated());
    adminCall("POST", "/api/v1/admin/professionals", creation("pm.dup@test.local", "PM-15", "PM-PROF-DUP2"))
        .andExpect(status().isConflict());
    adminCall("POST", "/api/v1/admin/professionals", creation("pm.dup2@test.local", "PM-16", "PM-PROF-DUP"))
        .andExpect(status().isConflict());
  }

  @Test
  void hu014_ca01_assignsSeveralSpecialtiesWithoutDuplicates() throws Exception {
    adminCall("PUT", "/api/v1/admin/professionals/" + professional + "/specialties",
        "{\"assignments\":[{\"id\":" + activeSpecialty + ",\"primary\":true},"
        + "{\"id\":" + otherSpecialty + ",\"primary\":false}]}")
        .andExpect(status().isOk());

    Assertions.assertEquals(2, db.queryForObject(
        "SELECT COUNT(*) FROM professional_specialties WHERE professional_id=?",
        Integer.class, professional).intValue());

    adminCall("PUT", "/api/v1/admin/professionals/" + professional + "/specialties",
        "{\"assignments\":[{\"id\":" + activeSpecialty + ",\"primary\":true},"
        + "{\"id\":" + activeSpecialty + ",\"primary\":false}]}")
        .andExpect(status().isBadRequest());
  }

  @Test
  void hu014_ca02_exactlyOnePrimarySpecialtyIsRequired() throws Exception {
    String two = "{\"assignments\":[{\"id\":" + activeSpecialty + ",\"primary\":true},"
        + "{\"id\":" + otherSpecialty + ",\"primary\":true}]}";
    String none = "{\"assignments\":[{\"id\":" + activeSpecialty + ",\"primary\":false},"
        + "{\"id\":" + otherSpecialty + ",\"primary\":false}]}";

    adminCall("PUT", "/api/v1/admin/professionals/" + professional + "/specialties", two)
        .andExpect(status().isBadRequest());
    adminCall("PUT", "/api/v1/admin/professionals/" + professional + "/specialties", none)
        .andExpect(status().isBadRequest());

    Assertions.assertEquals(0, db.queryForObject(
        "SELECT COUNT(*) FROM professional_specialties WHERE professional_id=?",
        Integer.class, professional).intValue(), "una asignacion invalida no debe dejar rastro");
  }

  @Test
  void hu014_ca03_inactiveOrUnknownSpecialtyIsNotEligible() throws Exception {
    adminCall("PUT", "/api/v1/admin/professionals/" + professional + "/specialties",
        "{\"assignments\":[{\"id\":" + inactiveSpecialty + ",\"primary\":true}]}")
        .andExpect(status().isBadRequest());
    adminCall("PUT", "/api/v1/admin/professionals/" + professional + "/specialties",
        "{\"assignments\":[{\"id\":999999,\"primary\":true}]}")
        .andExpect(status().isBadRequest());
  }

  @Test
  void hu015_ca01_assignsBothFixedSites() throws Exception {
    adminCall("PUT", "/api/v1/admin/professionals/" + professional + "/locations",
        "{\"ids\":[" + hic + "," + icv + "]}")
        .andExpect(status().isOk());
    Assertions.assertEquals(2, db.queryForObject(
        "SELECT COUNT(*) FROM professional_locations WHERE professional_id=?",
        Integer.class, professional).intValue());

    adminCall("PUT", "/api/v1/admin/professionals/" + professional + "/locations", "{\"ids\":[999]}")
        .andExpect(status().isBadRequest());
  }

  @Test
  void hu015_ca03_deactivatedProfessionalIsNotOfferedNorCanPublish() throws Exception {
    adminCall("PUT", "/api/v1/admin/professionals/" + professional + "/specialties",
        "{\"assignments\":[{\"id\":" + activeSpecialty + ",\"primary\":true}]}")
        .andExpect(status().isOk());
    var date = TestData.futureDate();
    data.createBlock(professional, hic, date, java.time.LocalTime.of(8, 0), java.time.LocalTime.of(9, 0));

    String before = mvc.perform(get("/api/v1/availability?date=" + date + "&specialtyId=" + activeSpecialty)
            .header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    Assertions.assertTrue(before.contains("\"professionalId\":" + professional));

    adminCall("PATCH", "/api/v1/admin/professionals/" + professional + "/active", "{\"active\":false}")
        .andExpect(status().isOk());

    String after = mvc.perform(get("/api/v1/availability?date=" + date + "&specialtyId=" + activeSpecialty)
            .header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    Assertions.assertFalse(after.contains("\"professionalId\":" + professional),
        "un profesional desactivado no debe ofrecerse en disponibilidad");

    String professionalToken = "Bearer " + data.login("pm.pro@test.local");
    mvc.perform(post("/api/v1/professional/availability-blocks")
            .header(HttpHeaders.AUTHORIZATION, professionalToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"locationId\":" + hic + ",\"availableDate\":\"" + date.plusDays(1)
                + "\",\"startTime\":\"08:00\",\"endTime\":\"10:00\"}"))
        .andExpect(status().isForbidden());
  }
}
