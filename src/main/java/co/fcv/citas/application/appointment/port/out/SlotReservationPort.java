package co.fcv.citas.application.appointment.port.out;

import java.time.LocalDateTime;
import java.util.Optional;

import co.fcv.citas.domain.appointment.SlotPlan;

/**
 * Puerto de salida para la ocupacion de franjas.
 *
 * <p>El metodo que importa es {@link #claim}: es el que sostiene RN-01, y su contrato es
 * deliberadamente estrecho. No ofrece «comprobar si esta libre» y «ocupar» como dos operaciones,
 * porque entre una y otra cabe otra reserva. Ofrece una sola que ocupa <b>solo si</b> sigue libre y
 * dice cuantas consiguio.
 */
public interface SlotReservationPort {

  /**
   * Franja libre y elegible para esa especialidad, o vacio. «Elegible» incluye que el profesional
   * este activo y tenga la especialidad asignada (RN-07 y RN-08).
   */
  Optional<OpenSlot> findOpenSlot(long slotId, long specialtyId);

  /** Cuantas franjas libres hay en el rango que necesita el plan. */
  int countFreeSlots(long professionalId, long locationId, SlotPlan plan);

  /**
   * Ocupa las franjas del plan para la cita, <b>solo las que sigan libres</b>, y devuelve cuantas
   * ocupo. Si el numero no coincide con el esperado, quien llama debe deshacer: otra reserva se
   * adelanto.
   */
  int claim(long appointmentId, long professionalId, long locationId, SlotPlan plan);

  /** Libera todas las franjas de la cita. Se usa al cancelar y al rechazar. */
  void releaseAll(long appointmentId);

  /**
   * Libera las franjas de la cita en {@code from}..{@code to} salvo las que caen en el rango que
   * conserva. La exclusion importa al reprogramar con solapamiento: sin ella, mover una cita de 60
   * minutos media hora mas tarde liberaria un slot que la cita sigue necesitando.
   */
  void releaseExcept(long appointmentId, long professionalId,
                     LocalDateTime from, LocalDateTime to, SlotPlan keep);

  /** Una franja libre con el profesional y la sede a los que pertenece. */
  record OpenSlot(long slotId, LocalDateTime startAt, long professionalId, long locationId) {}
}
