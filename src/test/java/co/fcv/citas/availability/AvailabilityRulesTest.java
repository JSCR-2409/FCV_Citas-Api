package co.fcv.citas.availability;

import java.time.*;
import java.util.List;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.fcv.citas.support.TestData;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HU-019 — Consultar disponibilidad. Cubre CA-01 filtros, CA-02 duracion completa y CA-03 exclusiones.
 * Transaccional: cada prueba parte del mismo escenario y revierte al terminar, de modo que los
 * datos sembrados en setUp no colisionan entre metodos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.transaction.annotation.Transactional
class AvailabilityRulesTest {

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired ObjectMapper json;
  @Autowired jakarta.persistence.EntityManager em;

  TestData data;
  String patient;
  LocalDate date;
  long hic, icv, general30, special60, inactive60, professional, otherProfessional;

  @BeforeEach
  void setUp() throws Exception {
    data = new TestData(mvc, db, json, em);
    date = TestData.futureDate();
    hic = data.locationId("HIC");
    icv = data.locationId("ICV");

    general30 = data.createSpecialty("AV_GEN_30", "Disponibilidad general 30", 30, true, true);
    special60 = data.createSpecialty("AV_ESP_60", "Disponibilidad especializada 60", 60, false, true);
    inactive60 = data.createSpecialty("AV_INACT_60", "Disponibilidad inactiva 60", 60, false, false);

    long patientId = data.registerUser("av.patient@test.local", "AV-1");
    data.setRole(patientId, "USER");
    patient = "Bearer " + data.login("av.patient@test.local");

    long professionalUser = data.registerUser("av.pro@test.local", "AV-2");
    data.setRole(professionalUser, "PROFESSIONAL");
    professional = data.createProfessional(professionalUser, "AV-PROF-1");
    data.assignSpecialty(professional, general30, true);
    data.assignSpecialty(professional, special60, false);
    data.assignLocation(professional, hic);

    // Agenda del PRD: dos bloques el mismo dia, 08:00-12:00 y 14:00-17:00.
    data.createBlock(professional, hic, date, LocalTime.of(8, 0), LocalTime.of(12, 0));
    data.createBlock(professional, hic, date, LocalTime.of(14, 0), LocalTime.of(17, 0));

    // Una cita de 60 min ya ocupa 10:00 y 10:30.
    long taken = data.createAppointment(patientId, professional, hic, special60, "APPROVED",
        LocalDateTime.of(date, LocalTime.of(10, 0)), 60);
    data.occupySlots(taken, professional, LocalDateTime.of(date, LocalTime.of(10, 0)), 60);

    long otherUser = data.registerUser("av.pro2@test.local", "AV-3");
    data.setRole(otherUser, "PROFESSIONAL");
    otherProfessional = data.createProfessional(otherUser, "AV-PROF-2");
    data.assignSpecialty(otherProfessional, general30, true);
    data.assignLocation(otherProfessional, icv);
    data.createBlock(otherProfessional, icv, date, LocalTime.of(8, 0), LocalTime.of(9, 0));
  }

  private List<String> startsOffered(long specialtyId, String extraQuery) throws Exception {
    String body = mvc.perform(get("/api/v1/availability?date=" + date + "&specialtyId=" + specialtyId + extraQuery)
            .header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    return json.readTree(body).get("items").findValuesAsText("startAt").stream().sorted().toList();
  }

  @Test
  void ca01_filtersByProfessionalAndLocation() throws Exception {
    // Sin filtro de profesional aparecen los dos; filtrando, solo el pedido.
    Assertions.assertEquals(2, db.queryForObject(
        "SELECT COUNT(DISTINCT ab.professional_id) FROM availability_blocks ab WHERE ab.available_date=?",
        Integer.class, date).intValue());

    List<String> onlyOther = startsOffered(general30, "&professionalId=" + otherProfessional);
    Assertions.assertEquals(List.of(date + "T08:00:00", date + "T08:30:00"), onlyOther);

    List<String> onlyIcv = startsOffered(general30, "&locationId=" + icv);
    Assertions.assertEquals(onlyOther, onlyIcv);
  }

  @Test
  void ca02_sixtyMinutesRequiresTwoConsecutiveFreeSlots() throws Exception {
    List<String> offered = startsOffered(special60, "&professionalId=" + professional);

    // 09:30 queda fuera porque 10:00 esta ocupado; 11:30 y 16:30 quedan fuera por no tener
    // slot siguiente dentro del bloque. La tarde completa si es elegible.
    List<String> expected = List.of(
        date + "T08:00:00", date + "T08:30:00", date + "T09:00:00",
        date + "T11:00:00",
        date + "T14:00:00", date + "T14:30:00", date + "T15:00:00", date + "T15:30:00", date + "T16:00:00");

    Assertions.assertEquals(expected, offered);
  }

  @Test
  void ca02_thirtyMinutesOffersEveryFreeSlot() throws Exception {
    List<String> offered = startsOffered(general30, "&professionalId=" + professional);
    // 14 slots en los dos bloques, menos los 2 que ocupa la cita existente.
    Assertions.assertEquals(12, offered.size());
    Assertions.assertFalse(offered.contains(date + "T10:00:00"));
    Assertions.assertFalse(offered.contains(date + "T10:30:00"));
    Assertions.assertTrue(offered.contains(date + "T11:30:00"));
  }

  @Test
  void ca01_returnedTimesMatchStoredSlots() throws Exception {
    List<String> offered = startsOffered(general30, "&professionalId=" + professional);
    // La hora publicada debe ser la misma que la almacenada, sin desplazamiento de zona.
    Assertions.assertTrue(offered.contains(date + "T08:00:00"),
        "la primera franja almacenada es 08:00 y debe publicarse como 08:00, no desplazada");
  }

  @Test
  void ca03_excludesInactiveSpecialty() throws Exception {
    mvc.perform(get("/api/v1/availability?date=" + date + "&specialtyId=" + inactive60)
            .header(HttpHeaders.AUTHORIZATION, patient))
        .andExpect(status().isNotFound());
  }

  @Test
  void ca03_excludesSpecialtyNotAssociatedToProfessional() throws Exception {
    long unrelated = data.createSpecialty("AV_NO_ASOC_30", "Disponibilidad no asociada", 30, false, true);
    Assertions.assertTrue(startsOffered(unrelated, "").isEmpty());
  }

  @Test
  void ca03_excludesDeactivatedProfessional() throws Exception {
    data.deactivateProfessional(professional);
    Assertions.assertTrue(startsOffered(special60, "&professionalId=" + professional).isEmpty());
  }
}
