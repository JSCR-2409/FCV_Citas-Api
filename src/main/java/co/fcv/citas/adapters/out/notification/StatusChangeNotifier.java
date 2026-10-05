package co.fcv.citas.adapters.out.notification;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * HU-033 — Notifica a n8n un cambio de estado relevante, para que WF-002 envie el correo.
 *
 * <p>Tres decisiones de diseno, todas por la misma razon: una automatizacion de notificacion no
 * puede influir en la operacion clinica.
 *
 * <ol>
 *   <li><b>Asincrono.</b> El envio ocurre fuera del hilo de la peticion, de modo que un n8n lento no
 *       alarga la respuesta que espera el usuario.
 *   <li><b>Sin propagar errores.</b> Si el webhook falla, se registra y nada mas. Que n8n este caido
 *       no puede impedir que un ADMIN apruebe una cita.
 *   <li><b>Desactivado por defecto.</b> Sin {@code app.integrations.status-webhook-url} no se
 *       intenta ningun envio, asi que el entorno de desarrollo y las pruebas no dependen de una
 *       instancia externa.
 * </ol>
 *
 * <p>La peticion va autenticada con un JWT de vida corta en {@code Authorization: Bearer}, firmado
 * HS256 con {@code app.integrations.webhook-secret}. n8n lo verifica de forma nativa con su
 * autenticacion JWT del nodo Webhook, de modo que el secreto vive en una credencial de n8n y no en
 * el JSON versionado.
 *
 * <p>Antes se enviaba un HMAC-SHA256 del cuerpo en {@code X-Signature}, y se retiro porque
 * <b>nadie lo verificaba</b>: un nodo Code necesitaria el secreto para comprobarlo, y meterlo ahi lo
 * dejaria dentro del JSON. Una firma que nadie comprueba no es una defensa, es decoracion. El JWT
 * cubre lo que importa: sin el secreto no se puede construir una peticion valida, y el {@code exp}
 * limita la ventana de reutilizacion.
 */
@Component
public class StatusChangeNotifier {

  private static final Logger log = LoggerFactory.getLogger(StatusChangeNotifier.class);

  private final JdbcTemplate db;
  private final ObjectMapper json;
  private final String webhookUrl;
  private final String signingSecret;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  /** Un solo hilo demonio: el orden de los eventos importa y el volumen del laboratorio es bajo. */
  private final ExecutorService sender = Executors.newSingleThreadExecutor(runnable -> {
    var thread = new Thread(runnable, "status-webhook");
    thread.setDaemon(true);
    return thread;
  });

  /** HS256 exige una clave de 256 bits: un secreto mas corto hace fallar la firma. */
  private static final int MIN_SECRET_LENGTH = 32;

  public StatusChangeNotifier(JdbcTemplate db, ObjectMapper json,
                              org.springframework.core.env.Environment env) {
    this.db = db;
    this.json = json;
    String url = env.getProperty("app.integrations.status-webhook-url", "");
    String secret = env.getProperty("app.integrations.webhook-secret", "");

    // La configuracion se valida una vez al arrancar y no en cada evento. Antes un secreto corto
    // lanzaba WeakKeyException por cada notificacion, de modo que el webhook no funcionaba nunca y
    // lo unico que lo delataba era un aviso por cita. Un error de configuracion debe verse al
    // arrancar.
    if (!url.isBlank() && secret.length() < MIN_SECRET_LENGTH) {
      log.error("Webhook de estados DESACTIVADO: app.integrations.webhook-secret tiene {} caracteres"
          + " y HS256 necesita al menos {}. Configure un secreto mas largo.",
          secret.length(), MIN_SECRET_LENGTH);
      url = "";
    }
    this.webhookUrl = url;
    this.signingSecret = secret;
  }

  /**
   * Solo esta activo con URL y con un secreto utilizable. Enviar el evento sin firmar seria peor que
   * no enviarlo: cualquiera que conozca la URL podria provocar correos a nombre de la institucion.
   */
  public boolean enabled() { return !webhookUrl.isBlank(); }

  /**
   * Publica el evento. Devuelve de inmediato.
   *
   * @param event tipo de suceso: APPOINTMENT_DECIDED, RESCHEDULE_DECIDED o APPOINTMENT_CANCELLED
   */
  public void publish(String event, long appointmentId, String status, String reason) {
    if (!enabled()) return;
    Map<String, Object> payload;
    try {
      payload = describe(event, appointmentId, status, reason);
    } catch (RuntimeException e) {
      // Si no se puede describir el evento, se registra y se sigue: la operacion ya se completo.
      log.warn("No se pudo construir el evento {} de la cita {}: {}", event, appointmentId, e.getMessage());
      return;
    }
    sender.submit(() -> post(payload));
  }

  /** Reune lo que el correo necesita. Sin documento ni telefono: el envio es por correo. */
  private Map<String, Object> describe(String event, long appointmentId, String status, String reason) {
    var rows = db.query(
        "SELECT a.id,a.scheduled_start_at startAt,st.code statusCode,"
        + "u.first_name patientNames,u.last_name patientSurnames,u.email patientEmail,"
        + "d.first_name doctorNames,d.last_name doctorSurnames,"
        + "sp.name specialtyName,l.name locationName"
        + " FROM appointments a"
        + " JOIN appointment_statuses st ON st.id=a.status_id"
        + " JOIN users u ON u.id=a.patient_user_id"
        + " JOIN professionals pr ON pr.id=a.professional_id"
        + " JOIN users d ON d.id=pr.user_id"
        + " JOIN specialties sp ON sp.id=a.specialty_id"
        + " JOIN locations l ON l.id=a.location_id WHERE a.id=?", (rs, n) -> {
          var row = new LinkedHashMap<String, Object>();
          row.put("event", event);
          row.put("appointmentId", rs.getLong("id"));
          row.put("status", status == null ? rs.getString("statusCode") : status);
          row.put("startAt", rs.getObject("startAt", java.time.LocalDateTime.class));
          row.put("patientName", rs.getString("patientNames") + " " + rs.getString("patientSurnames"));
          row.put("patientEmail", rs.getString("patientEmail"));
          row.put("professionalName", rs.getString("doctorNames") + " " + rs.getString("doctorSurnames"));
          row.put("specialtyName", rs.getString("specialtyName"));
          row.put("locationName", rs.getString("locationName"));
          row.put("reason", reason);
          return row;
        }, appointmentId);
    if (rows.isEmpty()) throw new IllegalStateException("la cita no existe");
    return rows.get(0);
  }

  private void post(Map<String, Object> payload) {
    try {
      String body = json.writeValueAsString(payload);
      var builder = HttpRequest.newBuilder(URI.create(webhookUrl))
          .timeout(Duration.ofSeconds(10))
          .header("Content-Type", "application/json");
      if (!signingSecret.isBlank()) builder.header("Authorization", "Bearer " + bearer());
      HttpResponse<String> response = http.send(builder.POST(
          HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
          HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 300) {
        log.warn("El webhook de estados respondió {} para la cita {}",
            response.statusCode(), payload.get("appointmentId"));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      // La URL del webhook nunca se registra: puede llevar un identificador secreto en la ruta.
      log.warn("No fue posible notificar el cambio de estado de la cita {}: {}",
          payload.get("appointmentId"), e.getClass().getSimpleName());
    }
  }

  /**
   * JWT de dos minutos para autenticar la llamada al webhook. Dos minutos y no quince: el evento se
   * entrega de inmediato, asi que una ventana larga solo amplia el margen de reutilizacion si el
   * token se capturase.
   */
  private String bearer() {
    var now = java.time.Instant.now();
    return io.jsonwebtoken.Jwts.builder()
        .issuer("citas-api")
        .subject("status-webhook")
        .issuedAt(java.util.Date.from(now))
        .expiration(java.util.Date.from(now.plusSeconds(120)))
        .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
            signingSecret.getBytes(StandardCharsets.UTF_8)))
        .compact();
  }
}
