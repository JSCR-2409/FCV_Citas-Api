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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HU-020 — Reservar cita general y HU-021 — Solicitar cita especializada.
 * Cubre aprobacion automatica, retencion de franja, consumo de dos slots en 60 minutos
 * y proteccion contra doble reserva.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AppointmentBookingRulesTest {

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired ObjectMapper json;
  @Autowired jakarta.persistence.EntityManager em;

  TestData data;
  String patientA, patientB;
  LocalDate date;
  long hic, general30, special30, special60, professional;

  @BeforeEach
  void setUp() throws Exception {
    data = new TestData(mvc, db, json, em);
    date = TestData.futureDate();
    hic = data.locationId("HIC");

    general30 = data.createSpecialty("BK_GEN_30", "Reserva general 30", 30, true, true);
    special30 = data.createSpecialty("BK_ESP_30", "Reserva especializada 30", 30, false, true);
    special60 = data.createSpecialty("BK_ESP_60", "Reserva especializada 60", 60, false, true);

    long a = data.registerUser("bk.a@test.local", "BK-1");
    data.setRole(a, "USER");
    patientA = "Bearer " + data.login("bk.a@test.local");
    long b = data.registerUser("bk.b@test.local", "BK-2");
    data.setRole(b, "USER");
    patientB = "Bearer " + data.login("bk.b@test.local");

    long professionalUser = data.registerUser("bk.pro@test.local", "BK-3");
    data.setRole(professionalUser, "PROFESSIONAL");
    professional = data.createProfessional(professionalUser, "BK-PROF-1");
    data.assignSpecialty(professional, general30, true);
    data.assignSpecialty(professional, special30, false);
    data.assignSpecialty(professional, special60, false);
    data.assignLocation(professional, hic);
    data.createBlock(professional, hic, date, LocalTime.of(8, 0), LocalTime.of(11, 0));
  }

  private long slot(int hour, int minute) {
    return data.slotAt(professional, LocalDateTime.of(date, LocalTime.of(hour, minute)));
  }

  private org.springframework.test.web.servlet.ResultActions book(String path, String token, long slotId, long specialtyId)
      throws Exception {
    return mvc.perform(post("/api/v1/appointments/" + path)
        .header(HttpHeaders.AUTHORIZATION, token)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"slotId\":" + slotId + ",\"specialtyId\":" + specialtyId + "}"));
  }

  @Test
  void hu020_ca01_generalBookingIsApprovedWithoutAdmin() throws Exception {
    long slotId = slot(8, 0);
    String body = book("general", patientA, slotId, general30)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("APPROVED"))
        .andReturn().getResponse().getContentAsString();

    long appointmentId = json.readTree(body).get("id").asLong();
    Assertions.assertEquals("APPROVED", data.statusOf(appointmentId));
    Assertions.assertEquals(appointmentId, data.appointmentOfSlot(slotId).longValue());
  }

  @Test
  void hu020_ca03_storedTimesMatchTheChosenSlot() throws Exception {
    long slotId = slot(8, 30);
    String body = book("general", patientA, slotId, general30).andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    long appointmentId = json.readTree(body).get("id").asLong();

    LocalDateTime stored = db.queryForObject("SELECT scheduled_start_at FROM appointments WHERE id=?",
        LocalDateTime.class, appointmentId);
    Assertions.assertEquals(LocalDateTime.of(date, LocalTime.of(8, 30)), stored,
        "la cita debe quedar en la franja elegida, sin desplazamiento de zona");
  }

  @Test
  void hu020_ca02_secondBookingOnTheSameSlotIsRejected() throws Exception {
    long slotId = slot(9, 0);
    book("general", patientA, slotId, general30).andExpect(status().isCreated());
    book("general", patientB, slotId, general30).andExpect(status().isConflict());

    Assertions.assertEquals(1, db.queryForObject(
        "SELECT COUNT(*) FROM appointments WHERE scheduled_start_at=?", Integer.class,
        LocalDateTime.of(date, LocalTime.of(9, 0))).intValue());
  }

  @Test
  void hu021_ca01_specializedIsRequestedAndHoldsTheSlot() throws Exception {
    long slotId = slot(9, 30);
    String body = book("specialized", patientA, slotId, special30)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("REQUESTED"))
        .andReturn().getResponse().getContentAsString();

    long appointmentId = json.readTree(body).get("id").asLong();
    Assertions.assertEquals("REQUESTED", data.statusOf(appointmentId));
    Assertions.assertEquals(appointmentId, data.appointmentOfSlot(slotId).longValue(),
        "la franja debe quedar retenida mientras la solicitud esta pendiente");
  }

  @Test
  void hu021_ca02_requestOnAHeldSlotIsRejected() throws Exception {
    long slotId = slot(10, 0);
    book("specialized", patientA, slotId, special30).andExpect(status().isCreated());
    book("specialized", patientB, slotId, special30).andExpect(status().isConflict());
  }

  @Test
  void hu019_ca02_sixtyMinutesConsumesTwoConsecutiveSlots() throws Exception {
    long first = slot(8, 0);
    String body = book("specialized", patientA, first, special60).andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    long appointmentId = json.readTree(body).get("id").asLong();

    Assertions.assertEquals(appointmentId, data.appointmentOfSlot(first).longValue());
    Assertions.assertEquals(appointmentId, data.appointmentOfSlot(slot(8, 30)).longValue());
    Assertions.assertNull(data.appointmentOfSlot(slot(9, 0)), "no debe consumir un tercer slot");
  }

  @Test
  void hu019_ca02_sixtyMinutesIsRejectedWhenTheNextSlotIsTaken() throws Exception {
    // Ocupa 09:00 y deja 08:30 libre: un inicio de 60 min en 08:30 ya no cabe.
    long blocker = data.createAppointment(
        db.queryForObject("SELECT id FROM users WHERE email=?", Long.class, "bk.b@test.local"),
        professional, hic, special30, "APPROVED", LocalDateTime.of(date, LocalTime.of(9, 0)), 30);
    data.occupySlots(blocker, professional, LocalDateTime.of(date, LocalTime.of(9, 0)), 30);

    book("specialized", patientA, slot(8, 30), special60).andExpect(status().isConflict());
    Assertions.assertNull(data.appointmentOfSlot(slot(8, 30)), "un rechazo no debe retener la franja");
  }

  @Test
  void hu020_ca01_generalEndpointRejectsASpecializedSpecialty() throws Exception {
    book("general", patientA, slot(10, 30), special30).andExpect(status().isBadRequest());
  }

  @Test
  void hu021_ca01_specializedEndpointRejectsAGeneralSpecialty() throws Exception {
    book("specialized", patientA, slot(10, 30), general30).andExpect(status().isBadRequest());
  }
}
