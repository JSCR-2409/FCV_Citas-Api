package co.fcv.citas.config;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Limite de peticiones de las dos rutas abusables. Los cupos se bajan a 3 aqui para no depender de
 * la configuracion general, que en pruebas esta elevada a proposito.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.rate-limit.recovery-per-minute=3",
    "app.rate-limit.integration-per-minute=3"
})
class RateLimitTest {

  @Autowired MockMvc mvc;

  /**
   * La recuperacion permite generar tokens en cantidad, y pedir uno nuevo invalida el anterior: sin
   * limite, cualquiera podria invalidar de forma repetida el token legitimo de otra persona.
   */
  @Test
  void theRecoveryEndpointStopsAfterItsQuota() throws Exception {
    for (int attempt = 1; attempt <= 3; attempt++) {
      mvc.perform(post("/api/auth/recovery/request").contentType(MediaType.APPLICATION_JSON)
              .content("{\"email\":\"limite@test.local\"}")
              .header("X-Forwarded-For", "203.0.113.10"))
          .andExpect(status().isOk());
    }
    mvc.perform(post("/api/auth/recovery/request").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"limite@test.local\"}")
            .header("X-Forwarded-For", "203.0.113.10"))
        .andExpect(status().isTooManyRequests())
        // Un cliente correcto puede esperar en lugar de reintentar en bucle.
        .andExpect(header().string("Retry-After", "60"));
  }

  /** El cupo es por IP: el abuso de una no puede dejar sin servicio a las demas. */
  @Test
  void theQuotaIsPerClientAndNotGlobal() throws Exception {
    for (int attempt = 1; attempt <= 4; attempt++) {
      mvc.perform(post("/api/auth/recovery/request").contentType(MediaType.APPLICATION_JSON)
          .content("{\"email\":\"otra@test.local\"}")
          .header("X-Forwarded-For", "203.0.113.20"));
    }
    mvc.perform(post("/api/auth/recovery/request").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"otra@test.local\"}")
            .header("X-Forwarded-For", "203.0.113.21"))
        .andExpect(status().isOk());
  }

  @Test
  void theIntegrationEndpointHasItsOwnQuota() throws Exception {
    for (int attempt = 1; attempt <= 3; attempt++) {
      mvc.perform(get("/api/v1/integrations/appointments/upcoming")
              .header("X-Forwarded-For", "203.0.113.30"))
          // Sigue siendo 403 porque no hay token: el limite no sustituye a la autorizacion.
          .andExpect(status().isForbidden());
    }
    mvc.perform(get("/api/v1/integrations/appointments/upcoming")
            .header("X-Forwarded-For", "203.0.113.30"))
        .andExpect(status().isTooManyRequests());
  }

  /**
   * El limite se aplica a las rutas abusables, no al resto de la API. Un login o una consulta de
   * catalogo no deben quedar atrapados por el cupo de recuperacion.
   */
  @Test
  void theRestOfTheApiIsNotRateLimited() throws Exception {
    for (int attempt = 1; attempt <= 8; attempt++) {
      mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
              .content("{\"email\":\"nadie@test.local\",\"password\":\"incorrecta\"}")
              .header("X-Forwarded-For", "203.0.113.40"))
          .andExpect(status().isUnauthorized());
    }
  }

  /** Los dos grupos llevan contadores separados: agotar uno no debe cerrar el otro. */
  @Test
  void theTwoBucketsAreIndependent() throws Exception {
    for (int attempt = 1; attempt <= 4; attempt++) {
      mvc.perform(post("/api/auth/recovery/request").contentType(MediaType.APPLICATION_JSON)
          .content("{\"email\":\"mezcla@test.local\"}")
          .header("X-Forwarded-For", "203.0.113.50"));
    }
    mvc.perform(get("/api/v1/integrations/appointments/upcoming")
            .header("X-Forwarded-For", "203.0.113.50"))
        .andExpect(status().isForbidden());
  }
}
