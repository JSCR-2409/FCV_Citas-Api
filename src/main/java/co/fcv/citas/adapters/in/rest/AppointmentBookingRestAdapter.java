package co.fcv.citas.adapters.in.rest;

import java.util.LinkedHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.fcv.citas.application.appointment.BookAppointmentUseCase;
import co.fcv.citas.domain.appointment.SpecialtyKind;

/**
 * Adaptador de entrada REST para HU-020 y HU-021.
 *
 * <p>Comparese con lo que habia: un unico metodo de 1.400 caracteres con la consulta de la
 * especialidad, las reglas de franja, el {@code INSERT}, el reclamo atomico y la auditoria en la
 * misma linea. Aqui el adaptador hace tres cosas y ninguna es de negocio: lee el usuario del token,
 * llama al caso de uso y da forma a la respuesta.
 *
 * <p>No queda ni un {@code try/catch}: las reglas incumplidas las traduce
 * {@link DomainRuleViolationHandler}.
 */
@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentBookingRestAdapter {

  private final BookAppointmentUseCase bookAppointment;

  public AppointmentBookingRestAdapter(BookAppointmentUseCase bookAppointment) {
    this.bookAppointment = bookAppointment;
  }

  /** RN-02: una especialidad general se auto-aprueba. */
  @PostMapping("/general")
  ResponseEntity<?> general(@RequestBody Booking body, Authentication auth) {
    return created(bookAppointment.book(command(body, auth), SpecialtyKind.GENERAL));
  }

  /** RN-03: una especializada nace pendiente y retiene la franja. */
  @PostMapping("/specialized")
  ResponseEntity<?> specialized(@RequestBody Booking body, Authentication auth) {
    return created(bookAppointment.book(command(body, auth), SpecialtyKind.SPECIALIZED));
  }

  private BookAppointmentUseCase.Command command(Booking body, Authentication auth) {
    long patient = Long.parseLong(auth.getName());
    return body == null
        ? new BookAppointmentUseCase.Command(null, null, patient)
        : new BookAppointmentUseCase.Command(body.slotId(), body.specialtyId(), patient);
  }

  /** La forma del cuerpo es la que ya consumia el frontend; el contrato no cambia. */
  private ResponseEntity<?> created(BookAppointmentUseCase.Result result) {
    var body = new LinkedHashMap<String, Object>();
    body.put("id", result.id());
    body.put("status", result.status().name());
    body.put("startAt", result.slots().start());
    body.put("endAt", result.slots().end());
    body.put("professionalId", result.professionalId());
    body.put("locationId", result.locationId());
    body.put("specialtyId", result.specialtyId());
    return ResponseEntity.status(HttpStatus.CREATED).body(body);
  }

  record Booking(Long slotId, Long specialtyId) {}
}
