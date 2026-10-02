package co.fcv.citas.appointment;

import java.time.LocalDateTime;
import java.time.LocalTime;

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
 * HU-025 — Agenda del profesional; HU-026 — Cierre de atencion; HU-031 — Auditoria de estados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfessionalAgendaAndAuditTest {

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired ObjectMapper json;
  @Autowired jakarta.persistence.EntityManager em;

  TestData data;
  String doctor, otherDoctor, patientToken, admin;
  long patientId, doctorUserId, professionalId, otherProfessionalId, specialtyId, locationId, otherLocationId;

  @BeforeEach
  void setUp() throws Exception {
    data = new TestData(mvc, db, json, em);
    patientId = data.registerUser("ag.patient@test.local", "AG-1");
    patientToken = "Bearer " + data.login("ag.patient@test.local");

    doctorUserId = data.registerUser("ag.doctor@test.local", "AG-2");
    data.setRole(doctorUserId, "PROFESSIONAL");
    doctor = "Bearer " + data.login("ag.doctor@test.local");
    professionalId = data.createProfessional(doctorUserId, "AG-DOC-1");

    long otherUser = data.registerUser("ag.other@test.local", "AG-3");
    data.setRole(otherUser, "PROFESSIONAL");
    otherDoctor = "Bearer " + data.login("ag.other@test.local");
    otherProfessionalId = data.createProfessional(otherUser, "AG-DOC-2");

    admin = data.bearer("ag.admin@test.local", "ADMIN", "AG-4");

    specialtyId = data.createSpecialty("AG_GEN", "General agenda", 30, true, true);
    var codes = db.queryForList("SELECT code FROM locations ORDER BY id", String.class);
    locationId = data.locationId(codes.get(0));
    otherLocationId = data.locationId(codes.get(1));
    for (long pro : new long[] {professionalId, otherProfessionalId}) {
      data.assignSpecialty(pro, specialtyId, true);
      data.assignLocation(pro, locationId);
      data.assignLocation(pro, otherLocationId);
    }
  }

  private long appointment(long pro, long loc, String status, LocalDateTime start) {
    return data.createAppointment(patientId, pro, loc, specialtyId, status, start, 30);
  }

  // --- HU-025 -------------------------------------------------------------------------------

  @Test
  void hu025_ca01_theAgendaShowsTodayAppointmentsWithPatientAndSlot() throws Exception {
    appointment(professionalId, locationId, "APPROVED", LocalDateTime.now().withHour(14).withMinute(0).withSecond(0).withNano(0));

    mvc.perform(get("/api/v1/professional/appointments").header(HttpHeaders.AUTHORIZATION, doctor))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(1))
        .andExpect(jsonPath("$.items[0].patientName").exists())
        .andExpect(jsonPath("$.items[0].startAt").exists())
        .andExpect(jsonPath("$.items[0].specialtyName").value("General agenda"))
        .andExpect(jsonPath("$.items[0].locationName").exists());
  }

  @Test
  void hu025_ca02_theAgendaCanBeQueriedByWeekAndFilteredBySite() throws Exception {
    var monday = TestData.futureDate();
    appointment(professionalId, locationId, "APPROVED", monday.atTime(9, 0));
    appointment(professionalId, otherLocationId, "APPROVED", monday.plusDays(2).atTime(9, 0));

    mvc.perform(get("/api/v1/professional/appointments?from=" + monday + "&to=" + monday.plusDays(6))
            .header(HttpHeaders.AUTHORIZATION, doctor))
        .andExpect(jsonPath("$.count").value(2));

    mvc.perform(get("/api/v1/professional/appointments?from=" + monday + "&to=" + monday.plusDays(6)
            + "&locationId=" + otherLocationId).header(HttpHeaders.AUTHORIZATION, doctor))
        .andExpect(jsonPath("$.count").value(1))
        .andExpect(jsonPath("$.items[0].locationId").value(otherLocationId));
  }

  /** RF-16: el profesional no puede ver datos de pacientes fuera de sus propias citas. */
  @Test
  void hu025_ca03_aProfessionalNeverSeesAnotherProfessionalsAppointments() throws Exception {
    var day = TestData.futureDate();
    appointment(otherProfessionalId, locationId, "APPROVED", day.atTime(9, 0));

    mvc.perform(get("/api/v1/professional/appointments?from=" + day + "&to=" + day)
            .header(HttpHeaders.AUTHORIZATION, doctor))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(0));
  }

  /** El filtro por profesional sale del token: no hay parametro con el que apuntar a otro. */
  @Test
  void hu025_ca03_aPatientHasNoAgendaAtAll() throws Exception {
    mvc.perform(get("/api/v1/professional/appointments").header(HttpHeaders.AUTHORIZATION, patientToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void hu025_cancelledAppointmentsAreNotPartOfTheAgendaByDefault() throws Exception {
    var day = TestData.futureDate();
    appointment(professionalId, locationId, "APPROVED", day.atTime(9, 0));
    appointment(professionalId, locationId, "CANCELLED", day.atTime(10, 0));

    mvc.perform(get("/api/v1/professional/appointments?from=" + day + "&to=" + day)
            .header(HttpHeaders.AUTHORIZATION, doctor))
        .andExpect(jsonPath("$.count").value(1));

    // Siguen siendo consultables si se piden explicitamente.
    mvc.perform(get("/api/v1/professional/appointments?from=" + day + "&to=" + day + "&status=CANCELLED")
            .header(HttpHeaders.AUTHORIZATION, doctor))
        .andExpect(jsonPath("$.count").value(1));
  }

  // --- HU-026 -------------------------------------------------------------------------------

  private org.springframework.test.web.servlet.ResultActions close(long id, String token, String outcome) throws Exception {
    return mvc.perform(patch("/api/v1/professional/appointments/" + id + "/attention")
        .header(HttpHeaders.AUTHORIZATION, token)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"outcome\":\"" + outcome + "\"}"));
  }

  @Test
  void hu026_ca01_apastApprovedAppointmentCanBeCompleted() throws Exception {
    long id = appointment(professionalId, locationId, "APPROVED", LocalDateTime.now().minusHours(2));

    close(id, doctor, "COMPLETED").andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));
    Assertions.assertEquals("COMPLETED", data.statusOf(id));
  }

  @Test
  void hu026_ca01_itCanAlsoBeMarkedAsNoShow() throws Exception {
    long id = appointment(professionalId, locationId, "APPROVED", LocalDateTime.now().minusHours(2));

    close(id, doctor, "NO_SHOW").andExpect(status().isOk());
    Assertions.assertEquals("NO_SHOW", data.statusOf(id));
  }

  @Test
  void hu026_ca02_aFutureAppointmentCannotBeClosed() throws Exception {
    long id = appointment(professionalId, locationId, "APPROVED", LocalDateTime.now().plusDays(3));

    close(id, doctor, "COMPLETED").andExpect(status().isConflict());
    Assertions.assertEquals("APPROVED", data.statusOf(id));
  }

  @Test
  void hu026_ca02_anAppointmentThatIsNotApprovedCannotBeClosed() throws Exception {
    long cancelled = appointment(professionalId, locationId, "CANCELLED", LocalDateTime.now().minusHours(2));
    long requested = appointment(professionalId, locationId, "REQUESTED", LocalDateTime.now().minusHours(2));

    close(cancelled, doctor, "COMPLETED").andExpect(status().isConflict());
    close(requested, doctor, "COMPLETED").andExpect(status().isConflict());
    Assertions.assertEquals("CANCELLED", data.statusOf(cancelled));
    Assertions.assertEquals("REQUESTED", data.statusOf(requested));
  }

  @Test
  void hu026_ca02_closingTwiceIsRejected() throws Exception {
    long id = appointment(professionalId, locationId, "APPROVED", LocalDateTime.now().minusHours(2));

    close(id, doctor, "COMPLETED").andExpect(status().isOk());
    // Un segundo cierre no debe poder reescribir el resultado de la atencion.
    close(id, doctor, "NO_SHOW").andExpect(status().isConflict());
    Assertions.assertEquals("COMPLETED", data.statusOf(id));
  }

  @Test
  void hu026_ca02_anotherProfessionalCannotCloseTheAppointment() throws Exception {
    long id = appointment(professionalId, locationId, "APPROVED", LocalDateTime.now().minusHours(2));

    close(id, otherDoctor, "COMPLETED").andExpect(status().isNotFound());
    Assertions.assertEquals("APPROVED", data.statusOf(id));
  }

  @Test
  void hu026_ca02_anInvalidOutcomeIsRejected() throws Exception {
    long id = appointment(professionalId, locationId, "APPROVED", LocalDateTime.now().minusHours(2));

    close(id, doctor, "CANCELLED").andExpect(status().isBadRequest());
    Assertions.assertEquals("APPROVED", data.statusOf(id));
  }

  /** La atencion ocurrio: liberar la franja permitiria reservar sobre un horario ya consumido. */
  @Test
  void hu026_ca03_closingDoesNotReleaseTheSlot() throws Exception {
    var day = java.time.LocalDate.now();
    data.createBlock(professionalId, locationId, day.plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0));
    long id = appointment(professionalId, locationId, "APPROVED", LocalDateTime.now().minusHours(2));
    long slot = data.slotAt(professionalId, day.plusDays(1).atTime(9, 0));
    db.update("UPDATE professional_slots SET appointment_id=? WHERE id=?", id, slot);

    close(id, doctor, "COMPLETED").andExpect(status().isOk());
    Assertions.assertEquals(id, data.appointmentOfSlot(slot).longValue(), "la franja debe seguir ocupada");
  }

  // --- HU-031 -------------------------------------------------------------------------------

  @Test
  void hu031_ca01_closingTheAttentionIsAudited() throws Exception {
    long id = appointment(professionalId, locationId, "APPROVED", LocalDateTime.now().minusHours(2));
    close(id, doctor, "NO_SHOW").andExpect(status().isOk());

    mvc.perform(get("/api/v1/admin/appointment-history?appointmentId=" + id)
            .header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(1))
        .andExpect(jsonPath("$.items[0].statusCode").value("NO_SHOW"))
        .andExpect(jsonPath("$.items[0].actorId").value(doctorUserId))
        .andExpect(jsonPath("$.items[0].changedAt").exists());
  }

  /** RF-19 exige actor y fuente; la fuente distingue quien origino el cambio. */
  @Test
  void hu031_ca01_eachEntryCarriesActorSourceAndTimestamp() throws Exception {
    long id = appointment(professionalId, locationId, "APPROVED", TestData.futureDate().atTime(9, 0));
    mvc.perform(patch("/api/v1/me/appointments/" + id + "/cancel").header(HttpHeaders.AUTHORIZATION, patientToken))
        .andExpect(status().isOk());

    mvc.perform(get("/api/v1/admin/appointment-history?appointmentId=" + id)
            .header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(jsonPath("$.items[0].changeSource").value("USER"))
        .andExpect(jsonPath("$.items[0].actorId").value(patientId))
        .andExpect(jsonPath("$.items[0].actorName").exists());
  }

  /** El historial debe reconstruir la secuencia completa, no solo el ultimo estado. */
  @Test
  void hu031_ca02_theHistoryOfAnAppointmentIsChronological() throws Exception {
    long id = appointment(professionalId, locationId, "APPROVED", TestData.futureDate().atTime(9, 0));
    db.update("INSERT INTO appointment_status_history(appointment_id,status_id,change_source,changed_at)"
        + " SELECT ?,s.id,'USER',? FROM appointment_statuses s WHERE s.code='REQUESTED'",
        id, LocalDateTime.now().minusDays(2));
    db.update("INSERT INTO appointment_status_history(appointment_id,status_id,change_source,changed_at)"
        + " SELECT ?,s.id,'ADMIN',? FROM appointment_statuses s WHERE s.code='APPROVED'",
        id, LocalDateTime.now().minusDays(1));

    mvc.perform(get("/api/v1/admin/appointment-history?appointmentId=" + id)
            .header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(jsonPath("$.count").value(2))
        .andExpect(jsonPath("$.items[0].statusCode").value("REQUESTED"))
        .andExpect(jsonPath("$.items[1].statusCode").value("APPROVED"));
  }

  /** Una transicion del sistema no tiene responsable humano y el contrato lo dice con null. */
  @Test
  void hu031_ca02_aSystemTransitionHasNoActor() throws Exception {
    long id = appointment(professionalId, locationId, "APPROVED", TestData.futureDate().atTime(9, 0));
    db.update("INSERT INTO appointment_status_history(appointment_id,status_id,change_source)"
        + " SELECT ?,s.id,'SYSTEM' FROM appointment_statuses s WHERE s.code='CANCELLED'", id);

    mvc.perform(get("/api/v1/admin/appointment-history?appointmentId=" + id)
            .header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(jsonPath("$.items[0].actorId").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.items[0].actorName").value(org.hamcrest.Matchers.nullValue()));
  }

  /**
   * CA-03. La auditoria expone quien decidio sobre una cita: no es informacion del paciente ni del
   * profesional, de modo que la ruta es administrativa.
   */
  @Test
  void hu031_ca03_onlyAdminCanReadTheHistory() throws Exception {
    mvc.perform(get("/api/v1/admin/appointment-history").header(HttpHeaders.AUTHORIZATION, patientToken))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/admin/appointment-history").header(HttpHeaders.AUTHORIZATION, doctor))
        .andExpect(status().isForbidden());
  }

  /** La reserva es la primera transicion: sin ella la auditoria empezaria por el segundo estado. */
  @Test
  void hu031_ca01_bookingItselfIsAudited() throws Exception {
    var day = TestData.futureDate();
    data.createBlock(professionalId, locationId, day, LocalTime.of(9, 0), LocalTime.of(10, 0));
    long slot = data.slotAt(professionalId, day.atTime(9, 0));

    String created = mvc.perform(post("/api/v1/appointments/general")
            .header(HttpHeaders.AUTHORIZATION, patientToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"slotId\":" + slot + ",\"specialtyId\":" + specialtyId + "}"))
        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    long id = json.readTree(created).get("id").asLong();

    mvc.perform(get("/api/v1/admin/appointment-history?appointmentId=" + id)
            .header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(jsonPath("$.count").value(1))
        .andExpect(jsonPath("$.items[0].statusCode").value("APPROVED"))
        .andExpect(jsonPath("$.items[0].changeSource").value("USER"));
  }
}
