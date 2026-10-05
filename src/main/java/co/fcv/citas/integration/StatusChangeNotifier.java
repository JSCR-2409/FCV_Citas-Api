package co.fcv.citas.integration;

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
 * <p>El payload va firmado con HMAC-SHA256 en {@code X-Signature} para que el workflow pueda
 * comprobar que viene de este backend. Sin firma, cualquiera que conozca la URL del webhook podria
 * provocar correos a nombre de la institucion.
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

  public StatusChangeNotifier(JdbcTemplate db, ObjectMapper json,
                              org.springframework.core.env.Environment env) {
    this.db = db;
    this.json = json;
    this.webhookUrl = env.getProperty("app.integrations.status-webhook-url", "");
    this.signingSecret = env.getProperty("app.integrations.webhook-secret", "");
  }

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
      if (!signingSecret.isBlank()) builder.header("X-Signature", sign(body));
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

  private String sign(String body) {
    try {
      var mac = javax.crypto.Mac.getInstance("HmacSHA256");
      mac.init(new javax.crypto.spec.SecretKeySpec(
          signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return java.util.HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("no se pudo firmar el evento", e);
    }
  }
}
