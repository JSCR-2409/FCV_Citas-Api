package co.fcv.citas.application.appointment.port.out;

/**
 * Puerto de salida para cerrar las reprogramaciones que quedan sin objeto.
 *
 * <p>Es un puerto propio y no un metodo del repositorio de citas porque el efecto es sobre otra cosa:
 * cancelar una cita no cambia la solicitud de reprogramacion, la deja sin sentido. Separarlo deja
 * visible que son dos escrituras con razones distintas.
 */
public interface PendingRescheduleClosurePort {

  /** Marca como canceladas las solicitudes {@code PENDING} de esa cita. */
  void cancelPendingFor(long appointmentId);
}
