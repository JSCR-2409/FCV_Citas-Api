package co.fcv.citas.professional;

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
 * HU-016 crear bloques, HU-017 modificar bloques futuros y HU-018 consultar calendario propio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AvailabilityBlockTest {

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired ObjectMapper json;
  @Autowired jakarta.persistence.EntityManager em;

  TestData data;
  String owner, other, patient;
  LocalDate date;
  long hic, icv, specialty, professional, otherProfessional, patientId;

  @BeforeEach
  void setUp() throws Exception {
    data = new TestData(mvc, db, json, em);
    date = TestData.futureDate();
    hic = data.locationId("HIC");
    icv = data.locationId("ICV");
    specialty = data.createSpecialty("AB_ESP_30", "Bloques 30", 30, false, true);

    patientId = data.registerUser("ab.patient@test.local", "AB-1");
    data.setRole(patientId, "USER");
    patient = "Bearer " + data.login("ab.patient@test.local");

    long ownerUser = data.registerUser("ab.pro@test.local", "AB-2");
    data.setRole(ownerUser, "PROFESSIONAL");
    professional = data.createProfessional(ownerUser, "AB-PROF-1");
    data.assignSpecialty(professional, specialty, true);
    data.assignLocation(professional, hic);   // ICV queda sin asignar a proposito
    owner = "Bearer " + data.login("ab.pro@test.local");

    long otherUser = data.registerUser("ab.pro2@test.local", "AB-3");
    data.setRole(otherUser, "PROFESSIONAL");
    otherProfessional = data.createProfessional(otherUser, "AB-PROF-2");
    data.assignSpecialty(otherProfessional, specialty, true);
    data.assignLocation(otherProfessional, icv);
    other = "Bearer " + data.login("ab.pro2@test.local");
  }

  private String payload(long locationId, LocalDate day, String from, String to) {
    return "{\"locationId\":" + locationId + ",\"availableDate\":\"" + day
        + "\",\"startTime\":\"" + from + "\",\"endTime\":\"" + to + "\"}";
  }

  private org.springframework.test.web.servlet.ResultActions create(String token, String body) throws Exception {
    return mvc.perform(post("/api/v1/professional/availability-blocks")
        .header(HttpHeaders.AUTHORIZATION, token)
        .contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private long createOk(String token, String body) throws Exception {
    String response = create(token, body).andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return json.readTree(response).get("id").asLong();
  }

  @Test
  void hu016_ca01_blockIsSplitIntoThirtyMinuteSlots() throws Exception {
    long id = createOk(owner, payload(hic, date, "08:00", "10:00"));

    Assertions.assertEquals(4, db.queryForObject(
        "SELECT COUNT(*) FROM professional_slots WHERE availability_block_id=?", Integer.class, id).intValue());
    Assertions.assertEquals(30, db.queryForObject(
        "SELECT TIMESTAMPDIFF(MINUTE, MIN(start_at), MIN(end_at)) FROM professional_slots"
        + " WHERE availability_block_id=?", Integer.class, id).intValue());
  }

  @Test
  void hu016_ca03_twoBlocksTheSameDayAndTheGapIsNotOffered() throws Exception {
    createOk(owner, payload(hic, date, "08:00", "12:00"));
    createOk(owner, payload(hic, date, "14:00", "17:00"));

    Assertions.assertEquals(14, db.queryForObject(
        "SELECT COUNT(*) FROM professional_slots ps JOIN availability_blocks ab"
        + " ON ab.id=ps.availability_block_id WHERE ab.professional_id=? AND ab.available_date=?",
        Integer.class, professional, date).intValue());

    // El intervalo intermedio no existe como franja.
    Assertions.assertEquals(0, db.queryForObject(
        "SELECT COUNT(*) FROM professional_slots ps JOIN availability_blocks ab"
        + " ON ab.id=ps.availability_block_id WHERE ab.professional_id=?"
        + " AND ps.start_at>=? AND ps.start_at<?",
        Integer.class, professional, LocalDateTime.of(date, LocalTime.of(12, 0)),
        LocalDateTime.of(date, LocalTime.of(14, 0))).intValue());
  }

  @Test
  void hu016_ca02_pastDateIsRejected() throws Exception {
    create(owner, payload(hic, LocalDate.now().minusDays(1), "08:00", "10:00"))
        .andExpect(status().isBadRequest());
    create(owner, payload(hic, LocalDate.now(), "08:00", "10:00"))
        .andExpect(status().isBadRequest());
    assertNoBlocks();
  }

  @Test
  void hu016_ca02_siteNotAssignedIsRejected() throws Exception {
    create(owner, payload(icv, date, "08:00", "10:00")).andExpect(status().isBadRequest());
    assertNoBlocks();
  }

  @Test
  void hu016_ca02_boundariesMustAlignToThirtyMinutes() throws Exception {
    create(owner, payload(hic, date, "08:15", "10:00")).andExpect(status().isBadRequest());
    create(owner, payload(hic, date, "08:00", "09:45")).andExpect(status().isBadRequest());
    create(owner, payload(hic, date, "10:00", "08:00")).andExpect(status().isBadRequest());
    assertNoBlocks();
  }

  @Test
  void hu016_ca02_overlappingBlockIsRejectedWithoutTouchingTheAgenda() throws Exception {
    long first = createOk(owner, payload(hic, date, "08:00", "12:00"));
    create(owner, payload(hic, date, "11:00", "13:00")).andExpect(status().isConflict());

    Assertions.assertEquals(1, db.queryForObject(
        "SELECT COUNT(*) FROM availability_blocks WHERE professional_id=? AND available_date=?",
        Integer.class, professional, date).intValue());
    Assertions.assertEquals(8, db.queryForObject(
        "SELECT COUNT(*) FROM professional_slots WHERE availability_block_id=?",
        Integer.class, first).intValue());
  }

  @Test
  void hu017_ca01_ownFutureBlockWithoutAppointmentsCanBeEditedAndDeleted() throws Exception {
    long id = createOk(owner, payload(hic, date, "08:00", "10:00"));

    mvc.perform(put("/api/v1/professional/availability-blocks/" + id)
            .header(HttpHeaders.AUTHORIZATION, owner)
            .contentType(MediaType.APPLICATION_JSON).content(payload(hic, date, "09:00", "11:00")))
        .andExpect(status().isOk());
    Assertions.assertEquals(LocalTime.of(9, 0), db.queryForObject(
        "SELECT start_time FROM availability_blocks WHERE id=?", LocalTime.class, id));

    mvc.perform(delete("/api/v1/professional/availability-blocks/" + id)
            .header(HttpHeaders.AUTHORIZATION, owner))
        .andExpect(status().isNoContent());
    Assertions.assertEquals(Boolean.FALSE, db.queryForObject(
        "SELECT active FROM availability_blocks WHERE id=?", Boolean.class, id),
        "el borrado es logico, para no perder el historico");
  }

  @Test
  void hu017_ca02_committedBlockIsProtected() throws Exception {
    long id = createOk(owner, payload(hic, date, "08:00", "10:00"));
    long appointment = data.createAppointment(patientId, professional, hic, specialty, "APPROVED",
        LocalDateTime.of(date, LocalTime.of(8, 0)), 30);
    data.occupySlots(appointment, professional, LocalDateTime.of(date, LocalTime.of(8, 0)), 30);

    mvc.perform(put("/api/v1/professional/availability-blocks/" + id)
            .header(HttpHeaders.AUTHORIZATION, owner)
            .contentType(MediaType.APPLICATION_JSON).content(payload(hic, date, "09:00", "11:00")))
        .andExpect(status().isConflict());
    mvc.perform(delete("/api/v1/professional/availability-blocks/" + id)
            .header(HttpHeaders.AUTHORIZATION, owner))
        .andExpect(status().isConflict());

    Assertions.assertEquals("APPROVED", data.statusOf(appointment), "la cita debe conservarse");
    Assertions.assertEquals(LocalTime.of(8, 0), db.queryForObject(
        "SELECT start_time FROM availability_blocks WHERE id=?", LocalTime.class, id));
  }

  @Test
  void hu017_ca03_anotherProfessionalsBlockCannotBeMutated() throws Exception {
    long foreign = createOk(other, payload(icv, date, "08:00", "09:00"));

    mvc.perform(put("/api/v1/professional/availability-blocks/" + foreign)
            .header(HttpHeaders.AUTHORIZATION, owner)
            .contentType(MediaType.APPLICATION_JSON).content(payload(hic, date, "10:00", "11:00")))
        .andExpect(status().isNotFound());
    mvc.perform(delete("/api/v1/professional/availability-blocks/" + foreign)
            .header(HttpHeaders.AUTHORIZATION, owner))
        .andExpect(status().isNotFound());

    Assertions.assertEquals(Boolean.TRUE, db.queryForObject(
        "SELECT active FROM availability_blocks WHERE id=?", Boolean.class, foreign));
  }

  @Test
  void hu017_ca03_pastBlockCannotBeMutated() throws Exception {
    // Un bloque pasado no se puede crear por la API, asi que se siembra directamente.
    long id = data.createBlock(professional, hic, LocalDate.now().minusDays(3),
        LocalTime.of(8, 0), LocalTime.of(9, 0));

    mvc.perform(put("/api/v1/professional/availability-blocks/" + id)
            .header(HttpHeaders.AUTHORIZATION, owner)
            .contentType(MediaType.APPLICATION_JSON).content(payload(hic, date, "08:00", "09:00")))
        .andExpect(status().isBadRequest());
    mvc.perform(delete("/api/v1/professional/availability-blocks/" + id)
            .header(HttpHeaders.AUTHORIZATION, owner))
        .andExpect(status().isBadRequest());
  }

  @Test
  void hu018_ca01_calendarShowsOwnBlocksWithDateTimeAndSite() throws Exception {
    createOk(owner, payload(hic, date, "08:00", "10:00"));

    mvc.perform(get("/api/v1/professional/availability-blocks?from=" + date + "&to=" + date)
            .header(HttpHeaders.AUTHORIZATION, owner))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].availableDate").value(date.toString()))
        .andExpect(jsonPath("$[0].startTime").value("08:00:00"))
        .andExpect(jsonPath("$[0].locationName").value("Hospital Internacional de Colombia (HIC)"));
  }

  @Test
  void hu018_ca02_calendarNeverExposesAnotherProfessionalsBlocks() throws Exception {
    createOk(other, payload(icv, date, "08:00", "09:00"));

    mvc.perform(get("/api/v1/professional/availability-blocks?from=" + date + "&to=" + date)
            .header(HttpHeaders.AUTHORIZATION, owner))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    // Un USER no es profesional: no puede obtener agenda ajena por esta via.
    mvc.perform(get("/api/v1/professional/availability-blocks?from=" + date + "&to=" + date)
            .header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void hu018_ca03_emptyRangeReturnsAnEmptyStateWithoutInventingAvailability() throws Exception {
    createOk(owner, payload(hic, date, "08:00", "10:00"));

    mvc.perform(get("/api/v1/professional/availability-blocks?from=" + date.plusMonths(6)
            + "&to=" + date.plusMonths(6).plusDays(5))
            .header(HttpHeaders.AUTHORIZATION, owner))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  private void assertNoBlocks() {
    Assertions.assertEquals(0, db.queryForObject(
        "SELECT COUNT(*) FROM availability_blocks WHERE professional_id=?",
        Integer.class, professional).intValue(), "un rechazo no debe modificar la agenda");
  }
}
