package co.fcv.citas.application.appointment.port.out;

import java.util.Optional;

import co.fcv.citas.domain.appointment.SpecialtyKind;

/**
 * Puerto de salida: lo que la aplicacion necesita saber de una especialidad para reservar.
 *
 * <p>Es una interfaz del lado de dentro, no del de fuera: la define la capa de aplicacion segun lo
 * que necesita, y el adaptador de persistencia se adapta a ella. Esa es la inversion de dependencia
 * que hace que el dominio no sepa que existe una base de datos.
 */
public interface SpecialtyCatalogPort {

  /** Especialidad <b>activa</b>, o vacio si no existe o esta desactivada. */
  Optional<EligibleSpecialty> findActive(long specialtyId);

  /** Lo minimo que la reserva necesita: cuanto dura y si exige decision administrativa. */
  record EligibleSpecialty(long id, int durationMinutes, SpecialtyKind kind) {}
}
