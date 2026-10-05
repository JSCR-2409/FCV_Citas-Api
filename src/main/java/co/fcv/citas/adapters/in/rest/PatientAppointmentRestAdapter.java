package co.fcv.citas.adapters.in.rest;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.fcv.citas.application.appointment.CancelAppointmentUseCase;
import co.fcv.citas.adapters.out.notification.StatusChangeNotifier;

/** Adaptador de entrada REST para HU-023. */
@RestController
@RequestMapping("/api/v1/me/appointments")
public class PatientAppointmentRestAdapter {

  private final CancelAppointmentUseCase cancelAppointment;
  private final StatusChangeNotifier notifier;

  public PatientAppointmentRestAdapter(CancelAppointmentUseCase cancelAppointment,
                                       StatusChangeNotifier notifier) {
    this.cancelAppointment = cancelAppointment;
    this.notifier = notifier;
  }

  @PatchMapping("/{id}/cancel")
  ResponseEntity<?> cancel(@PathVariable long id, Authentication auth) {
    var cancelled = cancelAppointment.cancel(id, Long.parseLong(auth.getName()));
    // HU-033: la notificacion va despues del caso de uso y no dentro, porque avisar a un sistema
    // externo no es parte de la regla de cancelar. Es asincrona y no propaga errores.
    notifier.publish("APPOINTMENT_CANCELLED", id, cancelled.status().name(), null);
    return ResponseEntity.ok(Map.of("id", id, "status", cancelled.status().name()));
  }
}
