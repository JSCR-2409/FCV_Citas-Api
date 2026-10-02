package co.fcv.citas.user;

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
 * HU-007 — Consultar y actualizar perfil; HU-022 — Consultar mis citas con filtros;
 * HU-023 — Cancelar cita, con su historial.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfileAndMyAppointmentsTest {

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired ObjectMapper json;
  @Autowired jakarta.persistence.EntityManager em;

  TestData data;
  String patient, otherPatient;
  long patientId, otherId, professionalId, specialtyId, locationId;

  @BeforeEach
  void setUp() throws Exception {
    data = new TestData(mvc, db, json, em);
    patientId = data.registerUser("prof.patient@test.local", "PRO-1");
    patient = "Bearer " + data.login("prof.patient@test.local");
    otherId = data.registerUser("prof.other@test.local", "PRO-2");
    otherPatient = "Bearer " + data.login("prof.other@test.local");

    long proUser = data.registerUser("prof.doctor@test.local", "PRO-3");
    professionalId = data.createProfessional(proUser, "PRO-DOC");
    specialtyId = data.createSpecialty("PRO_GEN", "General prueba", 30, true, true);
    locationId = data.locationId(locationCode());
    data.assignSpecialty(professionalId, specialtyId, true);
    data.assignLocation(professionalId, locationId);
  }

  private String locationCode() {
    return db.queryForList("SELECT code FROM locations ORDER BY id", String.class).get(0);
  }

  // --- HU-007 -------------------------------------------------------------------------------

  @Test
  void hu007_ca01_theUserSeesOnlyTheirOwnProfile() throws Exception {
    mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("prof.patient@test.local"))
        // El hash de la contrasena no forma parte del perfil bajo ninguna circunstancia.
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void hu007_ca02_anAllowedFieldIsPersistedAndVisibleAfterwards() throws Exception {
    mvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, patient)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"phone\":\"3151234567\",\"names\":\"Jhoan\"}"))
        .andExpect(status().isOk());

    mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(jsonPath("$.phone").value("3151234567"))
        .andExpect(jsonPath("$.names").value("Jhoan"));
  }

  /**
   * CA-03. El email y el documento identifican la cuenta: enviarlos en el cuerpo no debe cambiar
   * nada, porque el controlador no los lee. Si alguien los agregara al record, esta prueba falla.
   */
  @Test
  void hu007_ca03_identityFieldsCannotBeChangedThroughTheProfile() throws Exception {
    mvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, patient)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"phone\":\"3159999999\",\"email\":\"usurpado@test.local\","
                + "\"documentNumber\":\"999999\",\"active\":false,\"role\":\"ADMIN\"}"))
        .andExpect(status().isOk());

    mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(jsonPath("$.email").value("prof.patient@test.local"))
        .andExpect(jsonPath("$.documentNumber").value("PRO-1"))
        .andExpect(jsonPath("$.active").value(true))
        .andExpect(jsonPath("$.role").value("USER"));
  }

  /** CA-03. El id sale del token, de modo que no hay forma de dirigir el cambio a otra cuenta. */
  @Test
  void hu007_ca03_theUpdateCannotReachAnotherAccount() throws Exception {
    mvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, patient)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":" + otherId + ",\"phone\":\"3158888888\"}"))
        .andExpect(status().isOk());

    Assertions.assertEquals("3000000000",
        db.queryForObject("SELECT phone FROM users WHERE id=?", String.class, otherId));
  }

  @Test
  void hu007_ca03_anEmptyUpdateIsRejected() throws Exception {
    mvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, patient)
            .contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"   \"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void hu007_anonymousCallersCannotReadOrWriteTheProfile() throws Exception {
    mvc.perform(get("/api/v1/me")).andExpect(status().isForbidden());
    mvc.perform(patch("/api/v1/me").contentType(MediaType.APPLICATION_JSON)
            .content("{\"phone\":\"3150000000\"}"))
        .andExpect(status().isForbidden());
  }

  // --- HU-022 -------------------------------------------------------------------------------

  private long appointmentAt(long owner, String status, LocalDateTime start) {
    return data.createAppointment(owner, professionalId, locationId, specialtyId, status, start, 30);
  }

  @Test
  void hu022_ca01_eachAppointmentCarriesTheMinimumData() throws Exception {
    appointmentAt(patientId, "APPROVED", TestData.futureDate().atTime(9, 0));

    mvc.perform(get("/api/v1/me/appointments").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].facilityFullName").exists())
        .andExpect(jsonPath("$[0].doctorName").exists())
        .andExpect(jsonPath("$[0].specialty").value("General prueba"))
        .andExpect(jsonPath("$[0].startAt").exists())
        .andExpect(jsonPath("$[0].durationMinutes").value(30))
        .andExpect(jsonPath("$[0].status").value("APPROVED"));
  }

  @Test
  void hu022_ca02_theStatusFilterOnlyReturnsMatchingAppointments() throws Exception {
    appointmentAt(patientId, "APPROVED", TestData.futureDate().atTime(9, 0));
    appointmentAt(patientId, "CANCELLED", TestData.futureDate().atTime(10, 0));
    appointmentAt(patientId, "REQUESTED", TestData.futureDate().atTime(11, 0));

    mvc.perform(get("/api/v1/me/appointments?status=CANCELLED").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].status").value("CANCELLED"));

    // Varios estados en una sola llamada: es lo que necesita la vista de "activas".
    mvc.perform(get("/api/v1/me/appointments?status=APPROVED,REQUESTED").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void hu022_ca02_theDateFilterIsInclusiveOnBothEnds() throws Exception {
    var day = TestData.futureDate();
    appointmentAt(patientId, "APPROVED", day.atTime(16, 0));
    appointmentAt(patientId, "APPROVED", day.plusDays(5).atTime(9, 0));

    // Una cita de las 16:00 del dia "to" debe quedar dentro: el extremo es el dia, no la medianoche.
    mvc.perform(get("/api/v1/me/appointments?from=" + day + "&to=" + day)
            .header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(jsonPath("$.length()").value(1));

    mvc.perform(get("/api/v1/me/appointments?from=" + day + "&to=" + day.plusDays(5))
            .header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(jsonPath("$.length()").value(2));

    mvc.perform(get("/api/v1/me/appointments?from=" + day.plusDays(1))
            .header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void hu022_ca03_aRejectedAppointmentShowsItsReason() throws Exception {
    long id = appointmentAt(patientId, "REJECTED", TestData.futureDate().atTime(9, 0));
    db.update("UPDATE appointments SET reason=? WHERE id=?", "El profesional no atiende ese día", id);

    mvc.perform(get("/api/v1/me/appointments").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(jsonPath("$[0].reason").value("El profesional no atiende ese día"));
  }

  @Test
  void hu022_ca03_oneUserNeverSeesAnotherUsersAppointments() throws Exception {
    appointmentAt(patientId, "APPROVED", TestData.futureDate().atTime(9, 0));

    mvc.perform(get("/api/v1/me/appointments").header(HttpHeaders.AUTHORIZATION, otherPatient))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  // --- HU-023 -------------------------------------------------------------------------------

  @Test
  void hu023_ca01_cancellingReleasesTheSlotsAndRecordsTheChange() throws Exception {
    var day = TestData.futureDate();
    data.createBlock(professionalId, locationId, day, LocalTime.of(9, 0), LocalTime.of(10, 0));
    long id = appointmentAt(patientId, "APPROVED", day.atTime(9, 0));
    data.occupySlots(id, professionalId, day.atTime(9, 0), 30);
    long slot = data.slotAt(professionalId, day.atTime(9, 0));

    mvc.perform(patch("/api/v1/me/appointments/" + id + "/cancel").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    Assertions.assertEquals("CANCELLED", data.statusOf(id));
    Assertions.assertNull(data.appointmentOfSlot(slot), "la franja debe quedar libre");
    // CA-03: la cancelacion queda auditada con su actor y su fuente.
    Assertions.assertEquals(1, db.queryForObject(
        "SELECT COUNT(*) FROM appointment_status_history h JOIN appointment_statuses s ON s.id=h.status_id"
        + " WHERE h.appointment_id=? AND s.code='CANCELLED' AND h.changed_by_user_id=? AND h.change_source='USER'",
        Integer.class, id, patientId).intValue());
  }

  @Test
  void hu023_ca02_anotherUsersAppointmentCannotBeCancelled() throws Exception {
    long id = appointmentAt(patientId, "APPROVED", TestData.futureDate().atTime(9, 0));

    mvc.perform(patch("/api/v1/me/appointments/" + id + "/cancel").header(HttpHeaders.AUTHORIZATION, otherPatient))
        .andExpect(status().isConflict());
    Assertions.assertEquals("APPROVED", data.statusOf(id));
  }

  @Test
  void hu023_ca02_aPastAppointmentCannotBeCancelled() throws Exception {
    long id = appointmentAt(patientId, "APPROVED", LocalDateTime.now().minusDays(1).withHour(9).withMinute(0));

    mvc.perform(patch("/api/v1/me/appointments/" + id + "/cancel").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isConflict());
    Assertions.assertEquals("APPROVED", data.statusOf(id));
  }

  @Test
  void hu023_ca03_aCancelledAppointmentCannotBeCancelledAgain() throws Exception {
    long id = appointmentAt(patientId, "APPROVED", TestData.futureDate().atTime(9, 0));
    mvc.perform(patch("/api/v1/me/appointments/" + id + "/cancel").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isOk());

    // CANCELLED es terminal: un segundo intento no reactiva ni vuelve a registrar nada.
    mvc.perform(patch("/api/v1/me/appointments/" + id + "/cancel").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isConflict());
    Assertions.assertEquals(1, db.queryForObject(
        "SELECT COUNT(*) FROM appointment_status_history WHERE appointment_id=?", Integer.class, id).intValue());
  }

  @Test
  void hu023_ca02_aCompletedAppointmentIsTerminalForTheUser() throws Exception {
    long id = appointmentAt(patientId, "COMPLETED", TestData.futureDate().atTime(9, 0));

    mvc.perform(patch("/api/v1/me/appointments/" + id + "/cancel").header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isConflict());
    Assertions.assertEquals("COMPLETED", data.statusOf(id));
  }
}
