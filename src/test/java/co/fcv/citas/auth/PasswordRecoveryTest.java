package co.fcv.citas.auth;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.fcv.citas.support.TestData;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HU-006 — Recuperacion de contrasena con token temporal de un solo uso. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordRecoveryTest {

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired ObjectMapper json;
  @Autowired jakarta.persistence.EntityManager em;

  TestData data;

  @BeforeEach
  void setUp() {
    data = new TestData(mvc, db, json, em);
  }

  private String requestToken(String email) throws Exception {
    String body = mvc.perform(post("/api/auth/recovery/request").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\"}"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    var token = json.readTree(body).get("token");
    return token == null ? null : token.asText();
  }

  private org.springframework.test.web.servlet.ResultActions confirm(String token, String password) throws Exception {
    return mvc.perform(post("/api/auth/recovery/confirm").contentType(MediaType.APPLICATION_JSON)
        .content("{\"token\":\"" + token + "\",\"password\":\"" + password + "\"}"));
  }

  @Test
  void ca01_requestingRecoveryCreatesASingleLiveToken() throws Exception {
    data.registerUser("rec.one@test.local", "REC-1");
    Assertions.assertNotNull(requestToken("rec.one@test.local"), "el canal de laboratorio debe entregar el token");

    long id = db.queryForObject("SELECT id FROM users WHERE email=?", Long.class, "rec.one@test.local");
    Assertions.assertEquals(1, db.queryForObject(
        "SELECT COUNT(*) FROM password_reset_tokens WHERE user_id=? AND used_at IS NULL", Integer.class, id).intValue());
  }

  /** El token se guarda hasheado: con la tabla a la vista nadie puede reconstruir el valor. */
  @Test
  void ca01_theStoredTokenIsNotTheTokenItself() throws Exception {
    data.registerUser("rec.hash@test.local", "REC-2");
    String token = requestToken("rec.hash@test.local");

    Assertions.assertEquals(0, db.queryForObject(
        "SELECT COUNT(*) FROM password_reset_tokens WHERE token_hash=?", Integer.class, token).intValue());
    Assertions.assertEquals(1, db.queryForObject(
        "SELECT COUNT(*) FROM password_reset_tokens WHERE token_hash=?", Integer.class,
        AuthService.hash(token)).intValue());
  }

  /** Pedir un token nuevo invalida el anterior: un token filtrado deja de servir. */
  @Test
  void ca01_askingAgainInvalidatesThePreviousToken() throws Exception {
    data.registerUser("rec.again@test.local", "REC-3");
    String first = requestToken("rec.again@test.local");
    String second = requestToken("rec.again@test.local");

    confirm(first, "ClaveNueva123").andExpect(status().isBadRequest());
    confirm(second, "ClaveNueva123").andExpect(status().isNoContent());
  }

  /**
   * Responder 404 para un email no registrado convertiria el endpoint en un oraculo para
   * enumerar cuentas, asi que la respuesta es la misma en ambos casos.
   */
  @Test
  void ca01_anUnknownEmailDoesNotRevealThatItIsUnknown() throws Exception {
    mvc.perform(post("/api/auth/recovery/request").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"nadie@test.local\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").doesNotExist());
  }

  @Test
  void ca02_theNewPasswordWorksAndTheOldOneDoesNot() throws Exception {
    data.registerUser("rec.change@test.local", "REC-4");
    confirm(requestToken("rec.change@test.local"), "ClaveNueva123").andExpect(status().isNoContent());

    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"rec.change@test.local\",\"password\":\"ClaveNueva123\"}"))
        .andExpect(status().isOk());
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"rec.change@test.local\",\"password\":\"" + TestData.PASSWORD + "\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void ca02_theTokenCannotBeUsedTwice() throws Exception {
    data.registerUser("rec.once@test.local", "REC-5");
    String token = requestToken("rec.once@test.local");

    confirm(token, "ClaveNueva123").andExpect(status().isNoContent());
    confirm(token, "OtraClave456").andExpect(status().isBadRequest());

    // El segundo intento no debe haber cambiado nada: la contrasena sigue siendo la del primero.
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"rec.once@test.local\",\"password\":\"ClaveNueva123\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void ca03_anExpiredTokenIsRejectedAndThePasswordStands() throws Exception {
    data.registerUser("rec.exp@test.local", "REC-6");
    String token = requestToken("rec.exp@test.local");
    db.update("UPDATE password_reset_tokens SET expires_at=? WHERE token_hash=?",
        java.time.LocalDateTime.now().minusMinutes(1), AuthService.hash(token));

    confirm(token, "ClaveNueva123").andExpect(status().isBadRequest());
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"rec.exp@test.local\",\"password\":\"" + TestData.PASSWORD + "\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void ca03_anInventedTokenIsRejected() throws Exception {
    confirm("0".repeat(64), "ClaveNueva123").andExpect(status().isBadRequest());
  }

  @Test
  void ca03_aShortPasswordIsRejectedWithoutConsumingTheToken() throws Exception {
    data.registerUser("rec.short@test.local", "REC-7");
    String token = requestToken("rec.short@test.local");

    confirm(token, "corta").andExpect(status().isBadRequest());
    // El token sigue vivo: una contrasena invalida es un error del usuario, no un intento de uso.
    confirm(token, "ClaveNueva123").andExpect(status().isNoContent());
  }

  /**
   * Cambiar la contrasena revoca las sesiones abiertas. Sin esto, quien hubiera robado la cuenta
   * conservaria un refresh valido despues de que el titular la recuperase.
   */
  @Test
  void ca02_changingThePasswordRevokesLiveSessions() throws Exception {
    data.registerUser("rec.sess@test.local", "REC-8");
    String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"rec.sess@test.local\",\"password\":\"" + TestData.PASSWORD + "\"}"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    String refresh = json.readTree(login).get("refreshToken").asText();

    confirm(requestToken("rec.sess@test.local"), "ClaveNueva123").andExpect(status().isNoContent());

    mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + refresh + "\"}"))
        .andExpect(status().isUnauthorized());
  }

  /**
   * El endpoint antiguo cambiaba la contrasena de cualquier cuenta conociendo solo su email. Esta
   * prueba fija que no vuelva: si alguien lo reintroduce, falla.
   */
  @Test
  void theUnauthenticatedPasswordResetEndpointNoLongerExists() throws Exception {
    data.registerUser("rec.gone@test.local", "REC-9");
    mvc.perform(post("/api/auth/password-reset").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"rec.gone@test.local\",\"password\":\"Intruso12345\"}"))
        .andExpect(result -> Assertions.assertNotEquals(204, result.getResponse().getStatus(),
            "cambiar la contraseña solo con el email no puede volver a funcionar"));

    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"rec.gone@test.local\",\"password\":\"Intruso12345\"}"))
        .andExpect(status().isUnauthorized());
  }
}
