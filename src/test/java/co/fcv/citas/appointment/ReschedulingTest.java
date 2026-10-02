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
 * HU-024 solicitar reprogramación, HU-029 bandeja de reprogramaciones y HU-030 resolución.
 * El punto delicado es RN-10: mientras la solicitud está PENDING la cita retiene la franja original
 * y la propuesta, y cada decisión libera únicamente el lado que corresponde.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReschedulingTest {

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired ObjectMapper json;
  @Autowired jakarta.persistence.EntityManager em;

  TestData data;
  String patient, otherPatient, admin;
  LocalDate date;
  long hic, icv, specialty30, specialty60, professional, otherProfessional, patientId;

  @BeforeEach
  void setUp() throws Exception {
    data = new TestData(mvc, db, json, em);
    date = TestData.futureDate();
    hic = data.locationId("HIC");
    icv = data.locationId("ICV");
    specialty30 = data.createSpecialty("RS_ESP_30", "Reprogramacion 30", 30, false, true);
    specialty60 = data.createSpecialty("RS_ESP_60", "Reprogramacion 60", 60, false, true);

    patientId = data.registerUser("rs.patient@test.local", "RS-1");
    data.setRole(patientId, "USER");
    patient = "Bearer " + data.login("rs.patient@test.local");
    otherPatient = data.bearer("rs.other@test.local", "USER", "RS-2");
    admin = data.bearer("rs.admin@test.local", "ADMIN", "RS-3");

    long professionalUser = data.registerUser("rs.pro@test.local", "RS-4");
    data.setRole(professionalUser, "PROFESSIONAL");
    professional = data.createProfessional(professionalUser, "RS-PROF-1");
    data.assignSpecialty(professional, specialty30, true);
    data.assignSpecialty(professional, specialty60, false);
    data.assignLocation(professional, hic);
    data.createBlock(professional, hic, date, LocalTime.of(8, 0), LocalTime.of(12, 0));

    long otherUser = data.registerUser("rs.pro2@test.local", "RS-5");
    data.setRole(otherUser, "PROFESSIONAL");
    otherProfessional = data.createProfessional(otherUser, "RS-PROF-2");
    data.assignSpecialty(otherProfessional, specialty30, true);
    data.assignLocation(otherProfessional, icv);
    data.createBlock(otherProfessional, icv, date, LocalTime.of(8, 0), LocalTime.of(10, 0));
  }

  private long bookedAppointment(String status, LocalTime at, int minutes, long specialtyId) {
    long id = data.createAppointment(patientId, professional, hic, specialtyId, status,
        LocalDateTime.of(date, at), minutes);
    data.occupySlots(id, professional, LocalDateTime.of(date, at), minutes);
    return id;
  }

  private org.springframework.test.web.servlet.ResultActions ask(long appointmentId, long slotId, String token)
      throws Exception {
    return mvc.perform(post("/api/v1/me/appointments/" + appointmentId + "/reschedule-requests")
        .header(HttpHeaders.AUTHORIZATION, token)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"slotId\":" + slotId + "}"));
  }

  private long askOk(long appointmentId, LocalTime newAt) throws Exception {
    long slotId = data.slotAt(professional, LocalDateTime.of(date, newAt));
    String body = ask(appointmentId, slotId, patient).andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return json.readTree(body).get("id").asLong();
  }

  private org.springframework.test.web.servlet.ResultActions decide(long requestId, String body) throws Exception {
    return mvc.perform(patch("/api/v1/admin/reschedule-requests/" + requestId)
        .header(HttpHeaders.AUTHORIZATION, admin)
        .contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private Long holderOf(LocalTime at) {
    return data.appointmentOfSlot(data.slotAt(professional, LocalDateTime.of(date, at)));
  }

  private String statusOfRequest(long requestId) {
    return db.queryForObject("SELECT s.code FROM reschedule_requests rr"
        + " JOIN reschedule_request_statuses s ON s.id=rr.status_id WHERE rr.id=?", String.class, requestId);
  }

  // ---------- HU-024 ----------

  @Test
  void hu024_ca01_anApprovedFutureAppointmentCanBeRescheduled() throws Exception {
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 30, specialty30);
    long requestId = askOk(appointment, LocalTime.of(10, 0));

    Assertions.assertEquals("PENDING", statusOfRequest(requestId));
    Assertions.assertEquals(1, db.queryForObject(
        "SELECT COUNT(*) FROM reschedule_requests WHERE appointment_id=?", Integer.class, appointment).intValue());
  }

  @Test
  void hu024_ca02_bothSlotsStayHeldWhilePending() throws Exception {
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 30, specialty30);
    askOk(appointment, LocalTime.of(10, 0));

    Assertions.assertEquals(appointment, holderOf(LocalTime.of(8, 0)).longValue(),
        "la cita original debe conservar su franja");
    Assertions.assertEquals(appointment, holderOf(LocalTime.of(10, 0)).longValue(),
        "la franja propuesta debe quedar retenida");
    // Y la cita no se mueve antes de la decisión.
    Assertions.assertEquals(LocalDateTime.of(date, LocalTime.of(8, 0)), db.queryForObject(
        "SELECT scheduled_start_at FROM appointments WHERE id=?", LocalDateTime.class, appointment));
  }

  @Test
  void hu024_ca02_aHeldProposalIsNotOfferedToAnyoneElse() throws Exception {
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 30, specialty30);
    askOk(appointment, LocalTime.of(10, 0));

    long slotId = data.slotAt(professional, LocalDateTime.of(date, LocalTime.of(10, 0)));
    mvc.perform(post("/api/v1/appointments/specialized").header(HttpHeaders.AUTHORIZATION, otherPatient)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"slotId\":" + slotId + ",\"specialtyId\":" + specialty30 + "}"))
        .andExpect(status().isConflict());
  }

  @Test
  void hu024_ca03_onlyApprovedAppointmentsCanBeRescheduled() throws Exception {
    long requested = bookedAppointment("REQUESTED", LocalTime.of(8, 0), 30, specialty30);
    long slotId = data.slotAt(professional, LocalDateTime.of(date, LocalTime.of(10, 0)));
    ask(requested, slotId, patient).andExpect(status().isConflict());
    Assertions.assertNull(holderOf(LocalTime.of(10, 0)), "un rechazo no debe retener la franja");
  }

  @Test
  void hu024_ca03_aPastAppointmentCannotBeRescheduled() throws Exception {
    long past = data.createAppointment(patientId, professional, hic, specialty30, "APPROVED",
        LocalDateTime.now().minusDays(2), 30);
    long slotId = data.slotAt(professional, LocalDateTime.of(date, LocalTime.of(10, 0)));
    ask(past, slotId, patient).andExpect(status().isConflict());
  }

  @Test
  void hu024_ca03_changingProfessionalIsNotARescheduling() throws Exception {
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 30, specialty30);
    long foreignSlot = data.slotAt(otherProfessional, LocalDateTime.of(date, LocalTime.of(9, 0)));
    ask(appointment, foreignSlot, patient).andExpect(status().isConflict());
  }

  @Test
  void hu024_ca03_anotherPatientCannotRescheduleMyAppointment() throws Exception {
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 30, specialty30);
    long slotId = data.slotAt(professional, LocalDateTime.of(date, LocalTime.of(10, 0)));
    ask(appointment, slotId, otherPatient).andExpect(status().isConflict());
  }

  @Test
  void hu024_ca01_onlyOnePendingRequestPerAppointment() throws Exception {
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 30, specialty30);
    askOk(appointment, LocalTime.of(10, 0));
    long anotherSlot = data.slotAt(professional, LocalDateTime.of(date, LocalTime.of(11, 0)));
    ask(appointment, anotherSlot, patient).andExpect(status().isConflict());
    Assertions.assertNull(holderOf(LocalTime.of(11, 0)));
  }

  @Test
  void hu024_ca01_aSixtyMinuteAppointmentNeedsTwoConsecutiveSlots() throws Exception {
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 60, specialty60);
    // 11:30 es el último slot del bloque: no hay un segundo consecutivo.
    long lastSlot = data.slotAt(professional, LocalDateTime.of(date, LocalTime.of(11, 30)));
    ask(appointment, lastSlot, patient).andExpect(status().isConflict());

    long requestId = askOk(appointment, LocalTime.of(10, 0));
    Assertions.assertEquals("PENDING", statusOfRequest(requestId));
    Assertions.assertEquals(appointment, holderOf(LocalTime.of(10, 0)).longValue());
    Assertions.assertEquals(appointment, holderOf(LocalTime.of(10, 30)).longValue());
  }

  // ---------- HU-029 ----------

  @Test
  void hu029_ca01_trayShowsOnlyPendingRequests() throws Exception {
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 30, specialty30);
    long pending = askOk(appointment, LocalTime.of(10, 0));

    String body = mvc.perform(get("/api/v1/admin/reschedule-requests").header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    Assertions.assertTrue(body.contains("\"id\":" + pending));

    decide(pending, "{\"status\":\"APPROVED\"}").andExpect(status().isOk());
    String after = mvc.perform(get("/api/v1/admin/reschedule-requests").header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    Assertions.assertFalse(after.contains("\"id\":" + pending), "una solicitud resuelta sale de la bandeja");
  }

  @Test
  void hu029_ca02_trayCompareBothSlotsAndKeepsProfessionalAndSpecialty() throws Exception {
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 30, specialty30);
    askOk(appointment, LocalTime.of(10, 0));

    mvc.perform(get("/api/v1/admin/reschedule-requests").header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].previousStartAt").value(date + "T08:00:00"))
        .andExpect(jsonPath("$[0].requestedStartAt").value(date + "T10:00:00"))
        .andExpect(jsonPath("$[0].professionalCode").value("RS-PROF-1"))
        .andExpect(jsonPath("$[0].specialtyName").value("Reprogramacion 30"))
        .andExpect(jsonPath("$[0].durationMinutes").value(30))
        .andExpect(jsonPath("$[0].locationName").value("Hospital Internacional de Colombia (HIC)"))
        .andExpect(jsonPath("$[0].patientName").value("Nombre Apellido"));
  }

  @Test
  void hu029_ca03_trayAppliesFiltersAndRejectsOtherRoles() throws Exception {
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 30, specialty30);
    long requestId = askOk(appointment, LocalTime.of(10, 0));

    String byProfessional = mvc.perform(get("/api/v1/admin/reschedule-requests?professionalId=" + professional)
            .header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    Assertions.assertTrue(byProfessional.contains("\"id\":" + requestId));

    for (String query : new String[] {"?professionalId=" + otherProfessional, "?locationId=" + icv,
        "?specialtyId=" + specialty60, "?date=" + date.plusYears(5)}) {
      mvc.perform(get("/api/v1/admin/reschedule-requests" + query).header(HttpHeaders.AUTHORIZATION, admin))
          .andExpect(status().isOk())
          .andExpect(content().json("[]"));
    }

    mvc.perform(get("/api/v1/admin/reschedule-requests").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isForbidden());
  }

  // ---------- HU-030 ----------

  @Test
  void hu030_ca01_approvalMovesTheAppointmentAndFreesTheOldSlot() throws Exception {
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 30, specialty30);
    long requestId = askOk(appointment, LocalTime.of(10, 0));

    decide(requestId, "{\"status\":\"APPROVED\"}").andExpect(status().isOk());

    Assertions.assertEquals("APPROVED", statusOfRequest(requestId));
    Assertions.assertNull(holderOf(LocalTime.of(8, 0)), "la franja original debe liberarse");
    Assertions.assertEquals(appointment, holderOf(LocalTime.of(10, 0)).longValue(),
        "la cita se queda con la franja nueva");
    Assertions.assertEquals(LocalDateTime.of(date, LocalTime.of(10, 0)), db.queryForObject(
        "SELECT scheduled_start_at FROM appointments WHERE id=?", LocalDateTime.class, appointment));
    Assertions.assertEquals("APPROVED", data.statusOf(appointment));
  }

  @Test
  void hu030_ca02_rejectionKeepsTheOriginalAndFreesTheProposal() throws Exception {
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 30, specialty30);
    long requestId = askOk(appointment, LocalTime.of(10, 0));

    decide(requestId, "{\"status\":\"REJECTED\",\"reason\":\"Agenda comprometida esa franja\"}")
        .andExpect(status().isOk());

    Assertions.assertEquals("REJECTED", statusOfRequest(requestId));
    Assertions.assertEquals(appointment, holderOf(LocalTime.of(8, 0)).longValue(),
        "la cita original se conserva");
    Assertions.assertNull(holderOf(LocalTime.of(10, 0)), "la propuesta debe liberarse");
    Assertions.assertEquals(LocalDateTime.of(date, LocalTime.of(8, 0)), db.queryForObject(
        "SELECT scheduled_start_at FROM appointments WHERE id=?", LocalDateTime.class, appointment));
    Assertions.assertEquals("Agenda comprometida esa franja", db.queryForObject(
        "SELECT decision_reason FROM reschedule_requests WHERE id=?", String.class, requestId));
  }

  @Test
  void hu030_ca02_afterRejectionThePatientKeepsOrCancels() throws Exception {
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 30, specialty30);
    long requestId = askOk(appointment, LocalTime.of(10, 0));
    decide(requestId, "{\"status\":\"REJECTED\",\"reason\":\"no disponible\"}").andExpect(status().isOk());

    mvc.perform(patch("/api/v1/me/reschedule-requests/" + requestId + "/action")
            .header(HttpHeaders.AUTHORIZATION, patient)
            .contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"KEEP_APPOINTMENT\"}"))
        .andExpect(status().isOk());
    Assertions.assertEquals("APPROVED", data.statusOf(appointment));

    mvc.perform(patch("/api/v1/me/reschedule-requests/" + requestId + "/action")
            .header(HttpHeaders.AUTHORIZATION, patient)
            .contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"CANCEL_APPOINTMENT\"}"))
        .andExpect(status().isOk());
    Assertions.assertEquals("CANCELLED", data.statusOf(appointment));
    Assertions.assertNull(holderOf(LocalTime.of(8, 0)), "cancelar libera la franja");
  }

  @Test
  void hu030_ca03_anAlreadyResolvedRequestCannotBeDecidedAgain() throws Exception {
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 30, specialty30);
    long requestId = askOk(appointment, LocalTime.of(10, 0));
    decide(requestId, "{\"status\":\"APPROVED\"}").andExpect(status().isOk());

    decide(requestId, "{\"status\":\"REJECTED\",\"reason\":\"segunda decision indebida\"}")
        .andExpect(status().isConflict());

    Assertions.assertEquals("APPROVED", statusOfRequest(requestId));
    Assertions.assertEquals(appointment, holderOf(LocalTime.of(10, 0)).longValue(),
        "una segunda decisión no debe alterar las franjas");
    Assertions.assertEquals(LocalDateTime.of(date, LocalTime.of(10, 0)), db.queryForObject(
        "SELECT scheduled_start_at FROM appointments WHERE id=?", LocalDateTime.class, appointment));
  }

  @Test
  void hu030_ca03_rejectionWithoutReasonChangesNothing() throws Exception {
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 30, specialty30);
    long requestId = askOk(appointment, LocalTime.of(10, 0));

    decide(requestId, "{\"status\":\"REJECTED\"}").andExpect(status().isBadRequest());

    Assertions.assertEquals("PENDING", statusOfRequest(requestId));
    Assertions.assertEquals(appointment, holderOf(LocalTime.of(8, 0)).longValue());
    Assertions.assertEquals(appointment, holderOf(LocalTime.of(10, 0)).longValue());
  }

  @Test
  void hu030_ca03_aProposalThatIsNoLongerHeldCannotBeApproved() throws Exception {
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 30, specialty30);
    long requestId = askOk(appointment, LocalTime.of(10, 0));

    // Se simula la perdida de la retencion, que es la situacion de una solicitud sembrada sin
    // reservar su franja o de una liberacion externa.
    db.update("UPDATE professional_slots SET appointment_id=NULL WHERE id=?",
        data.slotAt(professional, LocalDateTime.of(date, LocalTime.of(10, 0))));

    decide(requestId, "{\"status\":\"APPROVED\"}").andExpect(status().isConflict());

    Assertions.assertEquals("PENDING", statusOfRequest(requestId), "sin cambios parciales");
    Assertions.assertEquals(appointment, holderOf(LocalTime.of(8, 0)).longValue(),
        "la cita conserva su franja original");
    Assertions.assertEquals(LocalDateTime.of(date, LocalTime.of(8, 0)), db.queryForObject(
        "SELECT scheduled_start_at FROM appointments WHERE id=?", LocalDateTime.class, appointment));
  }

  @Test
  void hu030_ca03_unknownRequestReturnsNotFound() throws Exception {
    decide(999999, "{\"status\":\"APPROVED\"}").andExpect(status().isNotFound());
  }

  @Test
  void hu030_ca01_overlappingProposalDoesNotFreeASlotStillNeeded() throws Exception {
    // Cita de 60 min en 08:00-09:00 que se mueve a 08:30-09:30: el slot 08:30 pertenece a las dos
    // franjas y no debe liberarse al aprobar.
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 60, specialty60);
    long requestId = askOk(appointment, LocalTime.of(8, 30));

    decide(requestId, "{\"status\":\"APPROVED\"}").andExpect(status().isOk());

    Assertions.assertNull(holderOf(LocalTime.of(8, 0)), "el slot que queda fuera se libera");
    Assertions.assertEquals(appointment, holderOf(LocalTime.of(8, 30)).longValue(),
        "el slot compartido por las dos franjas debe seguir retenido");
    Assertions.assertEquals(appointment, holderOf(LocalTime.of(9, 0)).longValue());
    Assertions.assertEquals(2, db.queryForObject(
        "SELECT COUNT(*) FROM professional_slots WHERE appointment_id=?", Integer.class, appointment).intValue(),
        "una cita de 60 minutos ocupa exactamente dos slots");
  }

  @Test
  void cancellingTheAppointmentClosesThePendingRequest() throws Exception {
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 30, specialty30);
    long requestId = askOk(appointment, LocalTime.of(10, 0));

    mvc.perform(patch("/api/v1/me/appointments/" + appointment + "/cancel")
            .header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isOk());

    Assertions.assertEquals("CANCELLED", statusOfRequest(requestId),
        "el ADMIN no debe seguir viendo una solicitud de una cita cancelada");
    Assertions.assertNull(holderOf(LocalTime.of(8, 0)));
    Assertions.assertNull(holderOf(LocalTime.of(10, 0)), "cancelar libera tambien la franja propuesta");
  }

  @Test
  void hu024_ca01_thePatientCanSeeTheStatusOfItsOwnRequests() throws Exception {
    long appointment = bookedAppointment("APPROVED", LocalTime.of(8, 0), 30, specialty30);
    long requestId = askOk(appointment, LocalTime.of(10, 0));
    decide(requestId, "{\"status\":\"REJECTED\",\"reason\":\"Sin disponibilidad operativa\"}")
        .andExpect(status().isOk());

    mvc.perform(get("/api/v1/me/reschedule-requests").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(requestId))
        .andExpect(jsonPath("$[0].status").value("REJECTED"))
        .andExpect(jsonPath("$[0].decisionReason").value("Sin disponibilidad operativa"));

    // Las solicitudes ajenas no se exponen.
    mvc.perform(get("/api/v1/me/reschedule-requests").header(HttpHeaders.AUTHORIZATION, otherPatient))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }
}
