package co.fcv.citas.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rotacion del token de integracion sin interrupcion.
 *
 * <p>`app.integrations.token` admite varios valores separados por coma: el primero es el vigente y
 * los siguientes son los que se estan retirando. Eso resuelve el problema real de un token fijo, que
 * no es su duracion sino que cambiarlo obligaba a elegir entre dejar la automatizacion caida un rato
 * o no cambiarlo nunca.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.integrations.token=token-vigente-de-prueba,token-en-retirada") // allow-secret: valores de prueba
class IntegrationTokenRotationTest {

  @Autowired MockMvc mvc;

  @Test
  void theCurrentTokenIsAccepted() throws Exception {
    mvc.perform(get("/api/v1/integrations/daily-summary")
            .header("X-Integration-Token", "token-vigente-de-prueba")) // allow-secret: valor de prueba
        .andExpect(status().isOk());
  }

  /** Durante la rotacion los dos valen: es lo que permite cambiarlo sin cortar el servicio. */
  @Test
  void theRetiringTokenIsStillAccepted() throws Exception {
    mvc.perform(get("/api/v1/integrations/daily-summary")
            .header("X-Integration-Token", "token-en-retirada")) // allow-secret: valor de prueba
        .andExpect(status().isOk());
  }

  @Test
  void aTokenAlreadyRemovedFromTheListIsRejected() throws Exception {
    mvc.perform(get("/api/v1/integrations/daily-summary")
            .header("X-Integration-Token", "token-ya-retirado")) // allow-secret: valor de prueba
        .andExpect(status().isForbidden());
  }

  /** La coma separa valores, de modo que la lista completa no es un token valido por si misma. */
  @Test
  void theWholeConfiguredListIsNotAToken() throws Exception {
    mvc.perform(get("/api/v1/integrations/daily-summary")
            .header("X-Integration-Token", "token-vigente-de-prueba,token-en-retirada")) // allow-secret: valor de prueba
        .andExpect(status().isForbidden());
  }
}
