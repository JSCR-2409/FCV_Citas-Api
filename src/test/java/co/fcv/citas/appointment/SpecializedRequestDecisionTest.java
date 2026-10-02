package co.fcv.citas.appointment;

import java.time.*;

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
 * HU-027 — Consultar bandeja especializada y HU-028 — Resolver solicitud especializada.
 * Cubre que la bandeja muestre solo pendientes con datos suficientes para decidir, que los
 * filtros se apliquen, que la autorizacion por rol se respete y que una solicitud ya resuelta
 * no pueda volver a decidirse.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SpecializedRequestDecisionTest {

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired ObjectMapper json;
  @Autowired jakarta.persistence.EntityManager em;

  TestData data;
  String admin, patient;
  LocalDate date;
  long hic, icv, general30, special30, professional, otherProfessional, patientId;

  @BeforeEach
  void setUp() throws Exception {
    data = new TestData(mvc, db, json, em);
    date = TestData.futureDate();
    hic = data.locationId("HIC");
    icv = data.locationId("ICV");

    general30 = data.createSpecialty("SR_GEN_30", "Decision general 30", 30, true, true);
    special30 = data.createSpecialty("SR_ESP_30", "Decision especializada 30", 30, false, true);

    long adminId = data.registerUser("sr.admin@test.local", "SR-1");
    data.setRole(adminId, "ADMIN");
    admin = "Bearer " + data.login("sr.admin@test.local");

    patientId = data.registerUser("sr.patient@test.local", "SR-2");
    data.setRole(patientId, "USER");
    patient = "Bearer " + data.login("sr.patient@test.local");

    long professionalUser = data.registerUser("sr.pro@test.local", "SR-3");
    data.setRole(professionalUser, "PROFESSIONAL");
    professional = data.createProfessional(professionalUser, "SR-PROF-1");
    data.assignSpecialty(professional, special30, true);
    data.assignSpecialty(professional, general30, false);
    data.assignLocation(professional, hic);
    data.createBlock(professional, hic, date, LocalTime.of(8, 0), LocalTime.of(10, 0));

    long otherUser = data.registerUser("sr.pro2@test.local", "SR-4");
    data.setRole(otherUser, "PROFESSIONAL");
    otherProfessional = data.createProfessional(otherUser, "SR-PROF-2");
    data.assignSpecialty(otherProfessional, special30, true);
    data.assignLocation(otherProfessional, icv);
    data.createBlock(otherProfessional, icv, date, LocalTime.of(8, 0), LocalTime.of(9, 0));
  }

  private long request(long professionalId, LocalTime at) {
    long slotId = data.slotAt(professionalId, LocalDateTime.of(date, at));
    long id = data.createAppointment(patientId, professionalId,
        professionalId == professional ? hic : icv, special30, "REQUESTED", LocalDateTime.of(date, at), 30);
    data.occupySlots(id, professionalId, LocalDateTime.of(date, at), 30);
    Assertions.assertEquals(id, data.appointmentOfSlot(slotId).longValue());
    return id;
  }

  private org.springframework.test.web.servlet.ResultActions decide(long id, String body) throws Exception {
    return mvc.perform(patch("/api/v1/admin/specialized-requests/" + id)
        .header(HttpHeaders.AUTHORIZATION, admin)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body));
  }

  @Test
  void hu027_ca01_trayListsOnlyPendingRequests() throws Exception {
    long pending = request(professional, LocalTime.of(8, 0));
    long generalApproved = data.createAppointment(patientId, professional, hic, general30, "APPROVED",
        LocalDateTime.of(date, LocalTime.of(8, 30)), 30);

    String body = mvc.perform(get("/api/v1/admin/specialized-requests").header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    Assertions.assertTrue(body.contains("\"id\":" + pending));
    Assertions.assertFalse(body.contains("\"id\":" + generalApproved),
        "una cita general aprobada no pertenece a la bandeja de solicitudes especializadas");
  }

  @Test
  void hu021_ca03_trayCarriesTheDataNeededToDecide() throws Exception {
    request(professional, LocalTime.of(8, 0));
    mvc.perform(get("/api/v1/admin/specialized-requests").header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].locationName").value("Hospital Internacional de Colombia (HIC)"))
        .andExpect(jsonPath("$[0].specialtyName").value("Decision especializada 30"))
        .andExpect(jsonPath("$[0].durationMinutes").value(30))
        .andExpect(jsonPath("$[0].professionalCode").value("SR-PROF-1"))
        .andExpect(jsonPath("$[0].startAt").value(date + "T08:00:00"));
  }

  @Test
  void hu027_ca02_trayAppliesFilters() throws Exception {
    long atHic = request(professional, LocalTime.of(8, 0));
    long atIcv = request(otherProfessional, LocalTime.of(8, 0));

    String byLocation = mvc.perform(get("/api/v1/admin/specialized-requests?locationId=" + icv)
            .header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    Assertions.assertTrue(byLocation.contains("\"id\":" + atIcv));
    Assertions.assertFalse(byLocation.contains("\"id\":" + atHic), "el filtro de sede debe aplicarse");

    String byProfessional = mvc.perform(get("/api/v1/admin/specialized-requests?professionalId=" + professional)
            .header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    Assertions.assertTrue(byProfessional.contains("\"id\":" + atHic));
    Assertions.assertFalse(byProfessional.contains("\"id\":" + atIcv), "el filtro de profesional debe aplicarse");

    String byImpossibleDate = mvc.perform(get("/api/v1/admin/specialized-requests?date=" + date.plusYears(5))
            .header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    Assertions.assertEquals("[]", byImpossibleDate, "el filtro de fecha debe aplicarse");
  }

  @Test
  void hu027_ca03_patientCannotReadTheAdminTray() throws Exception {
    mvc.perform(get("/api/v1/admin/specialized-requests").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isForbidden());
  }

  @Test
  void hu028_ca01_approvalKeepsTheHeldSlots() throws Exception {
    long id = request(professional, LocalTime.of(8, 0));
    long slotId = data.slotAt(professional, LocalDateTime.of(date, LocalTime.of(8, 0)));

    decide(id, "{\"status\":\"APPROVED\"}").andExpect(status().isOk());

    Assertions.assertEquals("APPROVED", data.statusOf(id));
    Assertions.assertEquals(id, data.appointmentOfSlot(slotId).longValue(), "aprobar no debe liberar la franja");
  }

  @Test
  void hu028_ca02_rejectionStoresTheReasonAndFreesTheSlots() throws Exception {
    long id = request(professional, LocalTime.of(8, 0));
    long slotId = data.slotAt(professional, LocalDateTime.of(date, LocalTime.of(8, 0)));

    decide(id, "{\"status\":\"REJECTED\",\"reason\":\"Agenda reasignada por junta medica\"}")
        .andExpect(status().isOk());

    Assertions.assertEquals("REJECTED", data.statusOf(id));
    Assertions.assertNull(data.appointmentOfSlot(slotId), "rechazar debe liberar la franja");
    Assertions.assertEquals("Agenda reasignada por junta medica", data.reasonOf(id),
        "el motivo del rechazo debe quedar persistido, no solo devuelto en la respuesta");
  }

  @Test
  void hu028_ca03_rejectionWithoutReasonIsRejected() throws Exception {
    long id = request(professional, LocalTime.of(8, 0));
    decide(id, "{\"status\":\"REJECTED\"}").andExpect(status().isBadRequest());
    Assertions.assertEquals("REQUESTED", data.statusOf(id));
  }

  @Test
  void hu028_ca03_anAlreadyResolvedRequestCannotBeDecidedAgain() throws Exception {
    long id = request(professional, LocalTime.of(8, 0));
    long slotId = data.slotAt(professional, LocalDateTime.of(date, LocalTime.of(8, 0)));
    decide(id, "{\"status\":\"APPROVED\"}").andExpect(status().isOk());

    decide(id, "{\"status\":\"REJECTED\",\"reason\":\"segunda decision indebida\"}")
        .andExpect(status().isConflict());

    Assertions.assertEquals("APPROVED", data.statusOf(id), "la decision previa debe conservarse");
    Assertions.assertEquals(id, data.appointmentOfSlot(slotId).longValue(),
        "una segunda decision no debe liberar la franja de una cita ya aprobada");
  }

  @Test
  void hu028_ca03_aGeneralAppointmentCannotBeResolvedThroughThisEndpoint() throws Exception {
    long slotId = data.slotAt(professional, LocalDateTime.of(date, LocalTime.of(9, 0)));
    long general = data.createAppointment(patientId, professional, hic, general30, "APPROVED",
        LocalDateTime.of(date, LocalTime.of(9, 0)), 30);
    data.occupySlots(general, professional, LocalDateTime.of(date, LocalTime.of(9, 0)), 30);

    decide(general, "{\"status\":\"REJECTED\",\"reason\":\"no deberia poder rechazarse aqui\"}")
        .andExpect(status().isConflict());

    Assertions.assertEquals("APPROVED", data.statusOf(general));
    Assertions.assertEquals(general, data.appointmentOfSlot(slotId).longValue(),
        "la franja de una cita general aprobada no debe liberarse desde la bandeja especializada");
  }

  @Test
  void hu028_ca03_unknownRequestReturnsNotFound() throws Exception {
    decide(999999, "{\"status\":\"APPROVED\"}").andExpect(status().isNotFound());
  }
}
