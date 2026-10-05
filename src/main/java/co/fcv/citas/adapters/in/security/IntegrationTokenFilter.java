package co.fcv.citas.adapters.in.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * HU-032 — Autenticacion de las automatizaciones n8n sobre {@code /api/v1/integrations/**}.
 *
 * <p>DECISION: un token de servicio en la cabecera {@code X-Integration-Token} en lugar de un JWT de
 * usuario. n8n no es una persona: no tiene perfil, no renueva sesion y no debe quedar atado a la
 * cuenta de nadie, porque desactivar a ese usuario romperia la automatizacion en silencio. El token
 * se configura por variable de entorno y nunca se versiona.
 *
 * <p>Si la variable no esta definida, el filtro no autentica nada y los endpoints de integracion
 * quedan cerrados. Un token por defecto versionado seria un token conocido.
 *
 * <h2>DECISION sobre la rotacion</h2>
 *
 * <p>El token sigue siendo fijo, pero deja de ser irrotable. {@code app.integrations.token} admite
 * <b>varios valores separados por coma</b>: el primero es el vigente y los siguientes son los que se
 * estan retirando. Eso resuelve el problema real de un token fijo, que no es su duracion sino que
 * cambiarlo obliga a elegir entre dejar la automatizacion caida un rato o no cambiarlo nunca.
 *
 * <p>El procedimiento de rotacion queda sin interrupcion: se pone el nuevo delante del viejo, se
 * actualiza la credencial en n8n, y cuando el log deja de avisar del token retirado se borra del
 * valor.
 *
 * <p>No se eligio una tabla de tokens con revocacion individual porque hay un unico consumidor: una
 * migracion, un endpoint y una pantalla para administrar una sola credencial es coste sin beneficio.
 * Tampoco una caducidad fija: un token que expira solo rompe una automatizacion desatendida en un
 * momento que nadie eligio.
 *
 * <p>Cada uso correcto se registra con su IP. El volumen esperado son un par de peticiones al dia,
 * de modo que el log es legible y un uso inesperado se nota, que es lo que de verdad hacia falta: un
 * token filtrado antes no dejaba ningun rastro.
 */
@Component
public class IntegrationTokenFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(IntegrationTokenFilter.class);

  static final String HEADER = "X-Integration-Token";
  /** Autoridad propia: no reutiliza ADMIN para que no herede los endpoints administrativos. */
  public static final String ROLE = "ROLE_INTEGRATION";

  /** El primero es el vigente; el resto se estan retirando. */
  private final List<byte[]> accepted;

  public IntegrationTokenFilter(Environment env) {
    String configured = env.getProperty("app.integrations.token", "");
    this.accepted = java.util.Arrays.stream(configured.split(","))
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .map(value -> value.getBytes(StandardCharsets.UTF_8))
        .toList();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/v1/integrations/");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {
    String presented = request.getHeader(HEADER);
    int matched = presented == null ? -1 : indexOf(presented);
    if (matched >= 0) {
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken("n8n-integration", null, // allow-secret: la credencial es literalmente null
              List.of(new SimpleGrantedAuthority(ROLE))));
      audit(request, matched);
    }
    // Un token invalido no se rechaza aqui: se deja pasar sin autenticar y Spring Security responde.
    // Asi no hay dos sitios que decidan el codigo de respuesta, y el token nunca se registra.
    chain.doFilter(request, response);
  }

  /**
   * Indice del token que coincide, o {@code -1}. Recorre todos y no corta en la primera coincidencia
   * para que el tiempo de respuesta no revele cual acepto.
   */
  private int indexOf(String presented) {
    byte[] bytes = presented.getBytes(StandardCharsets.UTF_8);
    int found = -1;
    for (int i = 0; i < accepted.size(); i++) {
      // isEqual compara en tiempo constante: comparar con equals filtra informacion por el tiempo.
      if (MessageDigest.isEqual(accepted.get(i), bytes)) found = i;
    }
    return found;
  }

  /** El token nunca se registra, solo su posicion y la IP de quien lo uso. */
  private void audit(HttpServletRequest request, int matched) {
    String ip = clientIp(request);
    if (matched == 0) {
      log.info("Integracion autenticada desde {} en {}", ip, request.getRequestURI());
    } else {
      log.warn("Integracion autenticada desde {} con un token EN RETIRADA (posicion {}):"
          + " termine la rotacion y quitelo de app.integrations.token", ip, matched);
    }
  }

  private static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      int comma = forwarded.indexOf(',');
      return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
    }
    String remote = request.getRemoteAddr();
    return remote == null ? "desconocida" : remote;
  }
}
