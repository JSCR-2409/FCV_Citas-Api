package co.fcv.citas.integration;

import java.time.LocalDateTime;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.fcv.citas.support.TestData;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HU-032 y HU-034 — Endpoints que consumen las automatizaciones n8n.
 *
 * <p>El token se fija aquí y no en el application.yml de pruebas: así queda claro que el valor es
 * de este escenario y no una credencial del proyecto.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.integrations.token=token-de-prueba-solo-para-h2") // allow-secret: valor de prueba
class IntegrationEndpointsTest {

  private static final String TOKEN = "token-de-prueba-solo-para-h2"; // allow-secret: valor de prueba

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired ObjectMapper json;
  @Autowired jakarta.persistence.EntityManager em;

  TestData data;
  long patientId, professionalId, specialtyId, locationId;
  String patientToken;

  @BeforeEach
  void setUp() throws Exception {
    data = new TestData(mvc, db, json, em);
    patientId = data.registerUser("int.patient@test.local", "INT-1");
    patientToken = "Bearer " + data.login("int.patient@test.local");
    long proUser = data.registerUser("int.doctor@test.local", "INT-2");
    professionalId = data.createProfessional(proUser, "INT-DOC");
    specialtyId = data.createSpecialty("INT_GEN", "General integración", 30, true, true);
    locationId = data.locationId(db.queryForList("SELECT code FROM locations ORDER BY id", String.class).get(0));
    data.assignSpecialty(professionalId, specialtyId, true);
    data.assignLocation(professionalId, locationId);
  }

  private long appointment(String status, LocalDateTime start) {
    return data.createAppointment(patientId, professionalId, locationId, specialtyId, status, start, 30);
  }

  // --- Autorización -------------------------------------------------------------------------

  @Test
  void theIntegrationEndpointIsClosedWithoutTheServiceToken() throws Exception {
    mvc.perform(get("/api/v1/integrations/appointments/upcoming"))
        .andExpect(status().isForbidden());
  }

  @Test
  void aWrongServiceTokenIsRejected() throws Exception {
    mvc.perform(get("/api/v1/integrations/appointments/upcoming")
            .header("X-Integration-Token", "token-equivocado")) // allow-secret: token deliberadamente invalido
        .andExpect(status().isForbidden());
  }

  /**
   * El token de servicio es para automatizaciones, no un atajo de privilegios: no debe abrir los
   * endpoints administrativos ni los del paciente.
   */
  @Test
  void theServiceTokenDoesNotOpenAnyOtherEndpoint() throws Exception {
    mvc.perform(get("/api/v1/admin/specialized-requests").header("X-Integration-Token", TOKEN))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/me").header("X-Integration-Token", TOKEN))
        .andExpect(status().isForbidden());
  }

  /** Y un usuario autenticado no alcanza la integración por tener sesión. */
  @Test
  void aLoggedInUserCannotReachTheIntegrationEndpoint() throws Exception {
    mvc.perform(get("/api/v1/integrations/appointments/upcoming")
            .header(HttpHeaders.AUTHORIZATION, patientToken))
        .andExpect(status().isForbidden());
  }

  // --- HU-032 CA-01 -------------------------------------------------------------------------

  @Test
  void ca01_anApprovedAppointmentInsideTheWindowIsSelected() throws Exception {
    appointment("APPROVED", LocalDateTime.now().plusHours(5));

    mvc.perform(get("/api/v1/integrations/appointments/upcoming").header("X-Integration-Token", TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.windowHours").value(24))
        .andExpect(jsonPath("$.count").value(1))
        .andExpect(jsonPath("$.items[0].patientEmail").value("int.patient@test.local"))
        .andExpect(jsonPath("$.items[0].specialtyName").value("General integración"))
        .andExpect(jsonPath("$.items[0].locationName").exists());
  }

  /** WF-001 lo exige de forma explícita: no recordar CANCELLED ni REJECTED. */
  @Test
  void ca01_cancelledAndRejectedAppointmentsAreNeverReminded() throws Exception {
    appointment("CANCELLED", LocalDateTime.now().plusHours(5));
    appointment("REJECTED", LocalDateTime.now().plusHours(6));
    appointment("REQUESTED", LocalDateTime.now().plusHours(7));

    mvc.perform(get("/api/v1/integrations/appointments/upcoming").header("X-Integration-Token", TOKEN))
        .andExpect(jsonPath("$.count").value(0));
  }

  @Test
  void ca01_appointmentsOutsideTheWindowAreNotSelected() throws Exception {
    appointment("APPROVED", LocalDateTime.now().plusHours(5));
    appointment("APPROVED", LocalDateTime.now().plusDays(4));
    // Una cita que ya pasó no se recuerda: el recordatorio es para lo que viene.
    appointment("APPROVED", LocalDateTime.now().minusHours(3));

    mvc.perform(get("/api/v1/integrations/appointments/upcoming").header("X-Integration-Token", TOKEN))
        .andExpect(jsonPath("$.count").value(1));

    mvc.perform(get("/api/v1/integrations/appointments/upcoming?withinHours=120")
            .header("X-Integration-Token", TOKEN))
        .andExpect(jsonPath("$.count").value(2));
  }

  /** Una ventana absurda no debe convertirse en un volcado de la tabla completa. */
  @Test
  void ca01_theWindowIsCappedAndNeverLessThanAnHour() throws Exception {
    mvc.perform(get("/api/v1/integrations/appointments/upcoming?withinHours=100000")
            .header("X-Integration-Token", TOKEN))
        .andExpect(jsonPath("$.windowHours").value(24 * 14));
    mvc.perform(get("/api/v1/integrations/appointments/upcoming?withinHours=0")
            .header("X-Integration-Token", TOKEN))
        .andExpect(jsonPath("$.windowHours").value(1));
  }

  /** El recordatorio se redacta con el correo; el documento y el teléfono no hacen falta. */
  @Test
  void ca03_thepayloadDoesNotCarryTheDocumentOrThePhone() throws Exception {
    appointment("APPROVED", LocalDateTime.now().plusHours(5));

    String body = mvc.perform(get("/api/v1/integrations/appointments/upcoming")
            .header("X-Integration-Token", TOKEN))
        .andReturn().getResponse().getContentAsString();

    Assertions.assertFalse(body.contains("INT-1"), "el número de documento no debe viajar");
    Assertions.assertFalse(body.contains("3000000000"), "el teléfono no debe viajar");
  }

  /**
   * CA-03. El endpoint es de solo lectura: una ejecución del workflow no puede cambiar el estado ni
   * la reserva de ninguna cita.
   */
  @Test
  void ca03_readingTheUpcomingAppointmentsChangesNothing() throws Exception {
    long id = appointment("APPROVED", LocalDateTime.now().plusHours(5));
    String before = data.statusOf(id);
    int historyBefore = db.queryForObject("SELECT COUNT(*) FROM appointment_status_history", Integer.class);

    mvc.perform(get("/api/v1/integrations/appointments/upcoming").header("X-Integration-Token", TOKEN))
        .andExpect(status().isOk());

    Assertions.assertEquals(before, data.statusOf(id));
    Assertions.assertEquals(historyBefore,
        db.queryForObject("SELECT COUNT(*) FROM appointment_status_history", Integer.class).intValue());
  }

  // --- HU-034 -------------------------------------------------------------------------------

  @Test
  void hu034_theDailySummaryGroupsBySiteStatusAndSpecialty() throws Exception {
    var today = java.time.LocalDate.now();
    appointment("APPROVED", today.atTime(9, 0));
    appointment("APPROVED", today.atTime(10, 0));
    appointment("NO_SHOW", today.atTime(11, 0));
    appointment("APPROVED", today.plusDays(3).atTime(9, 0));

    mvc.perform(get("/api/v1/integrations/daily-summary").header("X-Integration-Token", TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.byLocationAndStatus.length()").value(2))
        .andExpect(jsonPath("$.bySpecialty[0].total").value(3));
  }

  /** Un resumen son conteos: no necesita el nombre ni el correo de ningún paciente. */
  @Test
  void hu034_theSummaryCarriesNoPatientData() throws Exception {
    appointment("APPROVED", java.time.LocalDate.now().atTime(9, 0));

    String body = mvc.perform(get("/api/v1/integrations/daily-summary")
            .header("X-Integration-Token", TOKEN))
        .andReturn().getResponse().getContentAsString();

    Assertions.assertFalse(body.contains("int.patient@test.local"), "el resumen no debe llevar correos");
    Assertions.assertFalse(body.contains("Nombre"), "el resumen no debe llevar nombres de pacientes");
  }

  @Test
  void hu034_aSpecificDateCanBeRequested() throws Exception {
    var day = java.time.LocalDate.now().plusDays(6);
    appointment("APPROVED", day.atTime(9, 0));

    mvc.perform(get("/api/v1/integrations/daily-summary?date=" + day)
            .header("X-Integration-Token", TOKEN))
        .andExpect(jsonPath("$.date").value(day.toString()))
        .andExpect(jsonPath("$.total").value(1));
  }
}
