package co.fcv.citas.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

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
 */
@Component
public class IntegrationTokenFilter extends OncePerRequestFilter {

  static final String HEADER = "X-Integration-Token";
  /** Autoridad propia: no reutiliza ADMIN para que no herede los endpoints administrativos. */
  public static final String ROLE = "ROLE_INTEGRATION";

  private final byte[] expected;

  public IntegrationTokenFilter(Environment env) {
    String configured = env.getProperty("app.integrations.token", "");
    this.expected = configured.isBlank() ? null : configured.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/v1/integrations/");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {
    String presented = request.getHeader(HEADER);
    if (expected != null && presented != null && matches(presented)) {
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken("n8n-integration", null, // allow-secret: la credencial es literalmente null
              java.util.List.of(new SimpleGrantedAuthority(ROLE))));
    }
    // Un token invalido no se rechaza aqui: se deja pasar sin autenticar y Spring Security responde.
    // Asi no hay dos sitios que decidan el codigo de respuesta, y el token nunca se registra.
    chain.doFilter(request, response);
  }

  /** Comparacion en tiempo constante: comparar con equals filtra informacion por el tiempo. */
  private boolean matches(String presented) {
    return MessageDigest.isEqual(expected, presented.getBytes(StandardCharsets.UTF_8));
  }
}
