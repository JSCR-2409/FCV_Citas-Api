package co.fcv.citas.adapters.out.persistence;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import co.fcv.citas.application.appointment.port.out.SpecialtyCatalogPort;
import co.fcv.citas.domain.appointment.SpecialtyKind;

/**
 * Adaptador de salida del catalogo de especialidades.
 *
 * <p>Devuelve vacio para una especialidad inexistente <b>o</b> inactiva, de modo que quien llama no
 * necesita distinguir los dos casos: para reservar, una especialidad desactivada es tan inalcanzable
 * como una que no existe.
 */
@Component
class SpecialtyCatalogAdapter implements SpecialtyCatalogPort {

  private final JdbcTemplate db;

  SpecialtyCatalogAdapter(JdbcTemplate db) { this.db = db; }

  @Override
  public Optional<EligibleSpecialty> findActive(long specialtyId) {
    // queryForList y no queryForObject: este ultimo lanza EmptyResultDataAccessException cuando no
    // hay filas, de modo que una especialidad inactiva acabaria en un 500 en lugar del rechazo
    // que corresponde.
    var found = db.query(
        "SELECT id,appointment_duration_minutes,is_general FROM specialties WHERE id=? AND active=TRUE",
        (rs, n) -> new EligibleSpecialty(rs.getLong(1), rs.getInt(2), SpecialtyKind.of(rs.getBoolean(3))),
        specialtyId);
    return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
  }
}
