package co.fcv.citas.auth;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import co.fcv.citas.user.User;
import co.fcv.citas.user.UserRepository;

/**
 * HU-006 — Recuperacion de contrasena con token temporal de un solo uso.
 *
 * <p>Reemplaza al antiguo POST /api/auth/password-reset, que cambiaba la contrasena de cualquier
 * cuenta conociendo unicamente su email: no habia prueba de posesion del buzon.
 *
 * <p>DECISION sobre el canal de desarrollo (RF-03 lo permite explicitamente): el token se devuelve
 * en la respuesta solo si {@code app.recovery.expose-token} es true, que es el valor del perfil de
 * laboratorio. Con la bandera apagada la respuesta no lo lleva y el token solo existe en la tabla,
 * de modo que activar un envio real por correo no exige cambiar el contrato.
 */
@Service
public class PasswordRecoveryService {

  /** Vida del token. Corta a proposito: es una credencial de un solo uso. */
  static final int TTL_MINUTES = 30;

  private static final SecureRandom RANDOM = new SecureRandom();

  private final JdbcTemplate db;
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final co.fcv.citas.session.RefreshSessionRepository sessions;
  private final boolean exposeToken;

  public PasswordRecoveryService(JdbcTemplate db, UserRepository users, PasswordEncoder encoder, co.fcv.citas.session.RefreshSessionRepository sessions,
                                 org.springframework.core.env.Environment env) {
    this.db = db;
    this.users = users;
    this.encoder = encoder;
    this.sessions = sessions;
    this.exposeToken = Boolean.parseBoolean(env.getProperty("app.recovery.expose-token", "false"));
  }

  /**
   * CA-01. No distingue entre cuenta existente e inexistente: responder 404 convertiria el
   * endpoint en un oraculo para enumerar correos registrados. Devuelve el token solo cuando el
   * canal de laboratorio esta habilitado y la cuenta existe.
   */
  @Transactional
  public Optional<String> request(String email) {
    var account = users.findByEmailIgnoreCase(email.trim()).filter(User::isActive);
    if (account.isEmpty()) return Optional.empty();
    long userId = account.get().getId();

    // Un solo token vivo por cuenta: pedir uno nuevo invalida el anterior, de modo que un token
    // filtrado deja de servir en cuanto el titular legitimo vuelve a solicitar la recuperacion.
    db.update("UPDATE password_reset_tokens SET used_at=? WHERE user_id=? AND used_at IS NULL",
        LocalDateTime.now(), userId);

    byte[] raw = new byte[32];
    RANDOM.nextBytes(raw);
    String token = HexFormat.of().formatHex(raw);
    // Se guarda el hash, nunca el token: con la tabla a la vista nadie puede recuperar el valor.
    db.update("INSERT INTO password_reset_tokens(user_id,token_hash,expires_at) VALUES(?,?,?)",
        userId, AuthService.hash(token), LocalDateTime.now().plusMinutes(TTL_MINUTES));

    return exposeToken ? Optional.of(token) : Optional.empty();
  }

  /** CA-02 y CA-03. Consume el token y revoca las sesiones vivas de la cuenta. */
  @Transactional
  public void confirm(String token, String newPassword) {
    if (newPassword == null || newPassword.length() < 8) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contraseña inválida");
    }
    // La condicion del UPDATE es la validacion: vencido, ya usado o inexistente se comportan igual
    // y no hay ventana entre comprobar y consumir por la que dos peticiones puedan colarse.
    var rows = db.queryForList(
        "SELECT user_id FROM password_reset_tokens WHERE token_hash=? AND used_at IS NULL AND expires_at>?",
        Long.class, AuthService.hash(token), LocalDateTime.now());
    if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token inválido o vencido");
    long userId = rows.get(0);

    int consumed = db.update(
        "UPDATE password_reset_tokens SET used_at=? WHERE token_hash=? AND used_at IS NULL",
        LocalDateTime.now(), AuthService.hash(token));
    if (consumed == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token inválido o vencido");

    User user = users.findById(userId).filter(User::isActive)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "token inválido o vencido"));
    user.changePassword(encoder.encode(newPassword));
    users.save(user);

    // Cambiar la contrasena cierra las sesiones abiertas: si la cuenta estaba comprometida, el
    // atacante conserva un refresh valido mientras no se revoque. Se hace por el repositorio y no
    // con un UPDATE directo: un UPDATE por JDBC no se refleja en las entidades que JPA ya tiene
    // cargadas, de modo que dentro de la misma transaccion la sesion seguiria pareciendo viva.
    var live = sessions.findByUserIdAndRevokedFalse(userId);
    live.forEach(co.fcv.citas.session.RefreshSession::revoke);
    sessions.saveAll(live);
  }
}
