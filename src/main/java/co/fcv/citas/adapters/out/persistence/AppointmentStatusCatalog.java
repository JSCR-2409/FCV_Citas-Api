package co.fcv.citas.adapters.out.persistence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import co.fcv.citas.domain.appointment.AppointmentStatus;

/**
 * Traduce entre el enum del dominio y el identificador de {@code appointment_statuses}.
 *
 * <p>Existe porque el esquema es 3FN: el estado es una tabla de catalogo con su propio
 * identificador, no una cadena repetida en cada fila. El dominio razona con el enum y no deberia
 * conocer esos numeros; esta clase es la costura entre los dos, y vive en el adaptador.
 *
 * <p>Se memoriza al primer uso: el catalogo es fijo y precargado, de modo que resolverlo en cada
 * transicion seria una consulta por cambio de estado sin ninguna ganancia.
 */
@Component
public class AppointmentStatusCatalog {

  private final JdbcTemplate db;
  private final Map<AppointmentStatus, Short> idsByStatus = new ConcurrentHashMap<>();
  private final Map<Short, AppointmentStatus> statusesById = new ConcurrentHashMap<>();

  public AppointmentStatusCatalog(JdbcTemplate db) { this.db = db; }

  public Short idOf(AppointmentStatus status) {
    return idsByStatus.computeIfAbsent(status, code -> {
      var found = db.queryForList("SELECT id FROM appointment_statuses WHERE code=?",
          Short.class, code.name());
      if (found.isEmpty()) {
        // Un estado del PRD que no esta sembrado es un defecto de migracion, no un caso de negocio.
        throw new IllegalStateException("el catálogo de estados no contiene " + code.name());
      }
      return found.get(0);
    });
  }

  public AppointmentStatus statusOf(Short id) {
    return statusesById.computeIfAbsent(id, key -> {
      var found = db.queryForList("SELECT code FROM appointment_statuses WHERE id=?", String.class, key);
      if (found.isEmpty()) throw new IllegalStateException("estado de cita desconocido: " + key);
      return AppointmentStatus.of(found.get(0));
    });
  }
}
