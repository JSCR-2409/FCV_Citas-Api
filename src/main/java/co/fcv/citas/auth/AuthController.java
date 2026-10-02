package co.fcv.citas.auth;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth") public class AuthController { private final AuthService service; private final PasswordRecoveryService recovery; public AuthController(AuthService s,PasswordRecoveryService r){service=s;recovery=r;}
 @PostMapping("/register") ResponseEntity<Void> register(@Valid @RequestBody RegisterBody b){service.register(new AuthService.RegisterRequest(b.names,b.surnames,b.documentType,b.documentNumber,b.email,b.phone,b.password));return ResponseEntity.status(HttpStatus.CREATED).build();}
 @PostMapping("/login") AuthService.TokenResponse login(@Valid @RequestBody LoginBody b){return service.login(new AuthService.LoginRequest(b.email,b.password));}
 @PostMapping("/refresh") AuthService.TokenResponse refresh(@Valid @RequestBody RefreshBody b){return service.refresh(b.refreshToken);}
 @PostMapping("/logout") ResponseEntity<Void> logout(@Valid @RequestBody RefreshBody b){service.logout(b.refreshToken);return ResponseEntity.noContent().build();}
 // HU-006. La respuesta es siempre 200 con la misma forma: diferenciar cuenta existente de
 // inexistente permitiria enumerar los correos registrados. "token" solo viaja cuando el canal de
 // laboratorio esta habilitado (app.recovery.expose-token).
 @PostMapping("/recovery/request") java.util.Map<String,Object> recovery(@Valid @RequestBody EmailBody b){var token=recovery.request(b.email); var body=new java.util.LinkedHashMap<String,Object>(); body.put("message","Si la cuenta existe y está activa, se generó un enlace de recuperación."); token.ifPresent(t->body.put("token",t)); return body;}
 @PostMapping("/recovery/confirm") ResponseEntity<Void> confirmRecovery(@Valid @RequestBody RecoveryConfirmBody b){recovery.confirm(b.token,b.password);return ResponseEntity.noContent().build();}
 public static class RegisterBody{@NotBlank public String names;@NotBlank public String surnames;@NotBlank public String documentType;@NotBlank public String documentNumber;@Email @NotBlank public String email;@NotBlank public String phone;@NotBlank public String password;}
 public static class LoginBody{@Email @NotBlank public String email;@NotBlank public String password;}
 public static class RefreshBody{@NotBlank public String refreshToken;}
 public static class RecoveryConfirmBody{@NotBlank public String token;@NotBlank public String password;}
 public static class EmailBody{@Email @NotBlank public String email;}
}
