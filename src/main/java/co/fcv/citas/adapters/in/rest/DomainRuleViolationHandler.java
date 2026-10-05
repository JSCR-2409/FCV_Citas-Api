package co.fcv.citas.adapters.in.rest;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import co.fcv.citas.domain.shared.DomainRuleViolation;

/**
 * Traduce una regla de negocio incumplida al vocabulario de HTTP.
 *
 * <p>Esta clase es la razon de que el dominio pueda ignorar que existe HTTP. {@link DomainRuleViolation}
 * dice si el problema es la peticion o el estado del sistema, y aqui se decide que eso significa
 * {@code 400} o {@code 409}. Si manana el mismo dominio se expusiera por otra via, solo cambiaria
 * este archivo.
 *
 * <p>El cuerpo conserva la forma {@code {"message": ...}} que ya usaban los controladores, de modo
 * que el frontend y los contratos no cambian.
 */
@RestControllerAdvice
public class DomainRuleViolationHandler {

  @ExceptionHandler(DomainRuleViolation.class)
  ResponseEntity<Map<String, String>> handle(DomainRuleViolation violation) {
    HttpStatus status = violation.kind() == DomainRuleViolation.Kind.INVALID_REQUEST
        ? HttpStatus.BAD_REQUEST
        : HttpStatus.CONFLICT;
    return ResponseEntity.status(status).body(Map.of("message", violation.getMessage()));
  }
}
