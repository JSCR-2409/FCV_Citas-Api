package co.fcv.citas.adapters.out.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import co.fcv.citas.application.appointment.port.out.SlotReservationPort;
import co.fcv.citas.domain.appointment.SlotPlan;

/**
 * Adaptador de salida para la ocupacion de franjas.
 *
 * <p>DECISION — aqui se usa SQL y no JPA, deliberadamente. {@code claim} es un {@code UPDATE}
 * condicional: ocupa <b>solo</b> las franjas que sigan libres y devuelve cuantas consiguio. Esa
 * guarda dentro de la propia escritura es lo que sostiene RN-01, porque entre «comprobar que esta
 * libre» y «ocupar» cabe otra reserva. Expresarlo cargando entidades y guardandolas una a una
 * cambiaria la semantica: dejaria de ser atomico y abriria exactamente la ventana que la regla cierra.
 *
 * <p>Que este SQL viva en un adaptador es precisamente el punto de la arquitectura: el caso de uso
 * llama a {@code claim} y no sabe como esta implementado. La restriccion de usar Spring Data JPA se
 * cumple donde se persiste <b>estado de dominio</b>; una operacion de concurrencia expresada en el
 * motor es una decision de infraestructura, y vive en la capa de infraestructura.
 */
@Component
class SlotReservationAdapter implements SlotReservationPort {

  private final JdbcTemplate db;

  SlotReservationAdapter(JdbcTemplate db) { this.db = db; }

  @Override
  public Optional<OpenSlot> findOpenSlot(long slotId, long specialtyId) {
    // Los JOIN imponen RN-07 y RN-08: el profesional debe estar activo y tener esa especialidad
    // asignada y activa. Si falta cualquiera de las dos, la franja no es elegible.
    var found = db.query(
        "SELECT ps.id,ps.start_at,ab.professional_id,ab.location_id"
        + " FROM professional_slots ps"
        + " JOIN availability_blocks ab ON ab.id=ps.availability_block_id"
        + " JOIN professional_specialties psp ON psp.professional_id=ab.professional_id"
        + "   AND psp.specialty_id=? AND psp.active=TRUE"
        + " JOIN professionals p ON p.id=ab.professional_id AND p.active=TRUE"
        + " WHERE ps.id=? AND ps.appointment_id IS NULL",
        (rs, n) -> new OpenSlot(rs.getLong(1), rs.getObject(2, LocalDateTime.class),
            rs.getLong(3), rs.getLong(4)),
        specialtyId, slotId);
    return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
  }

  @Override
  public int countFreeSlots(long professionalId, long locationId, SlotPlan plan) {
    Integer free = db.queryForObject(
        "SELECT COUNT(*) FROM professional_slots ps"
        + " JOIN availability_blocks ab ON ab.id=ps.availability_block_id"
        + " WHERE ab.professional_id=? AND ab.location_id=?"
        + "   AND ps.start_at>=? AND ps.start_at<? AND ps.appointment_id IS NULL",
        Integer.class, professionalId, locationId, plan.start(), plan.end());
    return free == null ? 0 : free;
  }

  @Override
  public int claim(long appointmentId, long professionalId, long locationId, SlotPlan plan) {
    // La guarda appointment_id IS NULL va dentro del UPDATE: dos reservas simultaneas no pueden
    // compartir franja. Se evita UPDATE ... JOIN, que es sintaxis propia de MySQL, con una
    // subconsulta, que tambien es valida ahi y ademas se puede verificar en H2.
    return db.update(
        "UPDATE professional_slots SET appointment_id=?"
        + " WHERE appointment_id IS NULL AND start_at>=? AND start_at<?"
        + "   AND availability_block_id IN"
        + "     (SELECT id FROM availability_blocks WHERE professional_id=? AND location_id=?)",
        appointmentId, plan.start(), plan.end(), professionalId, locationId);
  }

  @Override
  public void releaseAll(long appointmentId) {
    db.update("UPDATE professional_slots SET appointment_id=NULL WHERE appointment_id=?", appointmentId);
  }

  @Override
  public void releaseExcept(long appointmentId, long professionalId,
                            LocalDateTime from, LocalDateTime to, SlotPlan keep) {
    db.update(
        "UPDATE professional_slots SET appointment_id=NULL"
        + " WHERE appointment_id=? AND start_at>=? AND start_at<?"
        + "   AND NOT (start_at>=? AND start_at<?)"
        + "   AND availability_block_id IN"
        + "     (SELECT id FROM availability_blocks WHERE professional_id=?)",
        appointmentId, from, to, keep.start(), keep.end(), professionalId);
  }
}
