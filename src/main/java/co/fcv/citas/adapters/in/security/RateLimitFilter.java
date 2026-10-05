package co.fcv.citas.adapters.in.security;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limite de peticiones para las dos rutas que se pueden abusar sin estar autenticado o con una sola
 * credencial.
 *
 * <p>Las dos tienen un abuso concreto que este filtro corta:
 *
 * <ul>
 *   <li>{@code /api/auth/recovery/**} permite generar tokens de recuperacion en cantidad. No
 *       dejaria leerlos, pero si llenar la tabla y invalidar de forma repetida el token legitimo de
 *       alguien, porque pedir uno nuevo invalida el anterior.
 *   <li>{@code /api/v1/integrations/**} permite probar el token de servicio en bucle.
 * </ul>
 *
 * <p>Deliberadamente simple: ventana fija en memoria, por IP y por grupo de ruta. Es suficiente para
 * el laboratorio y no añade dependencias. Sus dos limitaciones estan asumidas y documentadas: el
 * contador no se comparte entre instancias, y una ventana fija admite el doble del limite justo en
 * el salto entre ventanas. Un entorno real usaria un contador compartido y una ventana deslizante.
 */
/*
 * El orden va por delante de Spring Security, cuya cadena se registra en -100. Si se ejecutara
 * despues, Security ya habria respondido 403 a un token invalido y el limite no frenaria a quien
 * prueba credenciales en bucle, que es justo el abuso que debe cortar.
 */
@Component
@Order(-200)
public class RateLimitFilter extends OncePerRequestFilter {

  private static final Duration WINDOW = Duration.ofMinutes(1);

  private final Map<String, Counter> counters = new ConcurrentHashMap<>();
  /** Cupos por minuto. La recuperacion es mas estricta porque una persona la usa una vez. */
  private final int recoveryLimit;
  private final int integrationLimit;

  public RateLimitFilter(org.springframework.core.env.Environment env) {
    this.recoveryLimit = Integer.parseInt(env.getProperty("app.rate-limit.recovery-per-minute", "5"));
    this.integrationLimit = Integer.parseInt(env.getProperty("app.rate-limit.integration-per-minute", "60"));
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return bucketOf(request.getRequestURI()) == null;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {
    String bucket = bucketOf(request.getRequestURI());
    int limit = "recovery".equals(bucket) ? recoveryLimit : integrationLimit;
    String key = bucket + "|" + clientIp(request);

    if (!allow(key, limit)) {
      response.setStatus(429);
      response.setContentType("application/json;charset=UTF-8");
      // Retry-After en segundos: un cliente correcto puede esperar en lugar de reintentar en bucle.
      response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
      response.getWriter().write(
          "{\"message\":\"Demasiadas peticiones. Intente de nuevo en un momento.\"}");
      return;
    }
    chain.doFilter(request, response);
  }

  private static String bucketOf(String uri) {
    if (uri.startsWith("/api/auth/recovery/")) return "recovery";
    if (uri.startsWith("/api/v1/integrations/")) return "integrations";
    return null;
  }

  private boolean allow(String key, int limit) {
    var now = Instant.now();
    var counter = counters.compute(key, (k, existing) ->
        existing == null || existing.startedAt.plus(WINDOW).isBefore(now)
            ? new Counter(now)
            : existing);
    // La limpieza va aqui y no en un hilo aparte: sin ella el mapa crece con cada IP que aparezca.
    if (counters.size() > 10_000) {
      counters.entrySet().removeIf(e -> e.getValue().startedAt.plus(WINDOW).isBefore(now));
    }
    return counter.hits.incrementAndGet() <= limit;
  }

  /**
   * IP del cliente. Se lee {@code X-Forwarded-For} porque detras de un proxy o un tunel la IP
   * directa es la del proxy y todo el trafico compartiria un unico contador.
   */
  private static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      int comma = forwarded.indexOf(',');
      return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
    }
    String remote = request.getRemoteAddr();
    return remote == null ? "desconocida" : remote;
  }

  private static final class Counter {
    private final Instant startedAt;
    private final AtomicInteger hits = new AtomicInteger();
    private Counter(Instant startedAt) { this.startedAt = startedAt; }
  }
}
