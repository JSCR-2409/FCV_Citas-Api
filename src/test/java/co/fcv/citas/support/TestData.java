package co.fcv.citas.support;

import java.time.*;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Altas de datos para las pruebas de agenda y citas. El esquema de H2 lo construye Flyway
 * (V1-V8) y solo siembra roles, sedes, regimenes y estados: especialidades, profesionales,
 * bloques y slots los crea cada prueba con estos metodos.
 */
public class TestData {

  public static final String PASSWORD = "ClaveSegura123"; // allow-secret: solo pruebas en H2

  private final MockMvc mvc;
  private final JdbcTemplate db;
  private final ObjectMapper json;
  private final jakarta.persistence.EntityManager em;

  public TestData(MockMvc mvc, JdbcTemplate db, ObjectMapper json, jakarta.persistence.EntityManager em) {
    this.mvc = mvc;
    this.db = db;
    this.json = json;
    this.em = em;
  }

  public long registerUser(String email, String document) throws Exception {
    String body = "{\"names\":\"Nombre\",\"surnames\":\"Apellido\",\"documentType\":\"CC\","
        + "\"documentNumber\":\"" + document + "\",\"email\":\"" + email + "\","
        + "\"phone\":\"3000000000\",\"password\":\"" + PASSWORD + "\"}";
    mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status -> {
          if (status.getResponse().getStatus() != 201) {
            throw new IllegalStateException("registro fallido para " + email);
          }
        });
    return db.queryForObject("SELECT id FROM users WHERE email=?", Long.class, email);
  }

  /**
   * Reemplaza los roles del usuario, para que el claim del token sea exactamente el pedido.
   * El clear() es necesario: la prueba es transaccional y el alta via API dejo el User en la
   * sesion JPA con su rol original, asi que sin limpiarla el login leeria el rol obsoleto.
   */
  public void setRole(long userId, String roleCode) {
    // Primero se vuelca lo que JPA tiene pendiente del alta via API; si se hiciera despues,
    // reinsertaria el rol original y colisionaria con el que se fija aqui.
    em.flush();
    db.update("DELETE FROM user_roles WHERE user_id=?", userId);
    db.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,r.id FROM roles r WHERE r.code=?", userId, roleCode);
    em.clear();
  }

  public String login(String email) throws Exception {
    String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
        .andReturn().getResponse().getContentAsString();
    return json.readTree(body).get("accessToken").asText();
  }

  public String bearer(String email, String roleCode, String document) throws Exception {
    long id = registerUser(email, document);
    setRole(id, roleCode);
    return "Bearer " + login(email);
  }

  public long createSpecialty(String code, String name, int minutes, boolean general, boolean active) {
    db.update("INSERT INTO specialties(code,name,appointment_duration_minutes,is_general,requires_admin_approval,active)"
        + " VALUES(?,?,?,?,?,?)", code, name, minutes, general, !general, active);
    return db.queryForObject("SELECT id FROM specialties WHERE code=?", Long.class, code);
  }

  public long createProfessional(long userId, String code) {
    db.update("INSERT INTO professionals(user_id,professional_code,license_number,active) VALUES(?,?,?,TRUE)",
        userId, code, "LIC-" + code);
    return db.queryForObject("SELECT id FROM professionals WHERE professional_code=?", Long.class, code);
  }

  public void deactivateProfessional(long professionalId) {
    db.update("UPDATE professionals SET active=FALSE WHERE id=?", professionalId);
  }

  public void assignSpecialty(long professionalId, long specialtyId, boolean primary) {
    db.update("INSERT INTO professional_specialties(professional_id,specialty_id,is_primary,active) VALUES(?,?,?,TRUE)",
        professionalId, specialtyId, primary);
  }

  public void assignLocation(long professionalId, long locationId) {
    db.update("INSERT INTO professional_locations(professional_id,location_id) VALUES(?,?)", professionalId, locationId);
  }

  public long locationId(String code) {
    return db.queryForObject("SELECT id FROM locations WHERE code=?", Long.class, code);
  }

  /** Crea el bloque y lo discretiza en slots de 30 minutos, igual que hace el backend. */
  public long createBlock(long professionalId, long locationId, LocalDate date, LocalTime from, LocalTime to) {
    db.update("INSERT INTO availability_blocks(professional_id,location_id,available_date,start_time,end_time,active)"
        + " VALUES(?,?,?,?,?,TRUE)", professionalId, locationId, date, from, to);
    long blockId = db.queryForObject(
        "SELECT id FROM availability_blocks WHERE professional_id=? AND available_date=? AND start_time=?",
        Long.class, professionalId, date, from);
    for (LocalDateTime cursor = LocalDateTime.of(date, from), end = LocalDateTime.of(date, to);
        cursor.isBefore(end); cursor = cursor.plusMinutes(30)) {
      db.update("INSERT INTO professional_slots(availability_block_id,start_at,end_at) VALUES(?,?,?)",
          blockId, cursor, cursor.plusMinutes(30));
    }
    return blockId;
  }

  /** Inserta una cita ya existente, para construir escenarios con franjas ocupadas. */
  public long createAppointment(long patientUserId, long professionalId, long locationId, long specialtyId,
      String statusCode, LocalDateTime start, int minutes) {
    db.update("INSERT INTO appointments(patient_user_id,professional_id,location_id,specialty_id,status_id,"
        + "scheduled_start_at,scheduled_end_at,created_by_user_id)"
        + " SELECT ?,?,?,?,s.id,?,?,? FROM appointment_statuses s WHERE s.code=?",
        patientUserId, professionalId, locationId, specialtyId, start, start.plusMinutes(minutes),
        patientUserId, statusCode);
    return db.queryForObject("SELECT MAX(id) FROM appointments", Long.class);
  }

  /** Marca como ocupados los slots del profesional desde start durante los minutos indicados. */
  public void occupySlots(long appointmentId, long professionalId, LocalDateTime start, int minutes) {
    var ids = db.queryForList("SELECT ps.id FROM professional_slots ps"
        + " JOIN availability_blocks ab ON ab.id=ps.availability_block_id"
        + " WHERE ab.professional_id=? AND ps.start_at>=? AND ps.start_at<?",
        Long.class, professionalId, start, start.plusMinutes(minutes));
    for (Long id : ids) {
      db.update("UPDATE professional_slots SET appointment_id=? WHERE id=?", appointmentId, id);
    }
  }

  public long slotAt(long professionalId, LocalDateTime startAt) {
    return db.queryForObject("SELECT ps.id FROM professional_slots ps"
        + " JOIN availability_blocks ab ON ab.id=ps.availability_block_id"
        + " WHERE ab.professional_id=? AND ps.start_at=?", Long.class, professionalId, startAt);
  }

  public Long appointmentOfSlot(long slotId) {
    return db.queryForObject("SELECT appointment_id FROM professional_slots WHERE id=?", Long.class, slotId);
  }

  public String statusOf(long appointmentId) {
    return db.queryForObject("SELECT s.code FROM appointments a JOIN appointment_statuses s ON s.id=a.status_id"
        + " WHERE a.id=?", String.class, appointmentId);
  }

  public String reasonOf(long appointmentId) {
    return db.queryForObject("SELECT reason FROM appointments WHERE id=?", String.class, appointmentId);
  }

  public List<LocalDateTime> slotStarts(long professionalId) {
    return db.query("SELECT ps.start_at FROM professional_slots ps"
        + " JOIN availability_blocks ab ON ab.id=ps.availability_block_id"
        + " WHERE ab.professional_id=? ORDER BY ps.start_at",
        (rs, n) -> rs.getObject(1, LocalDateTime.class), professionalId);
  }

  /** Fecha futura estable para las pruebas: evita depender del dia en que se ejecutan. */
  public static LocalDate futureDate() {
    return LocalDate.now().plusDays(10);
  }
}
