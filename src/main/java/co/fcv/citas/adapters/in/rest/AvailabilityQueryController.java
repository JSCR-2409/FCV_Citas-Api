package co.fcv.citas.adapters.in.rest;

import java.time.*; import java.util.*; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.web.bind.annotation.*; import org.springframework.http.*;

@RestController @RequestMapping("/api/v1/availability")
public class AvailabilityQueryController {
  private final JdbcTemplate db; public AvailabilityQueryController(JdbcTemplate db){this.db=db;}
  @GetMapping
  ResponseEntity<?> search(@RequestParam LocalDate date,@RequestParam Long specialtyId,@RequestParam(required=false) Long locationId,@RequestParam(required=false) Long professionalId){
    // queryForObject lanza EmptyResultDataAccessException si no hay filas, nunca devuelve null:
    // una especialidad inexistente o inactiva debe responder 404, no 500.
    var durations=db.queryForList("SELECT appointment_duration_minutes FROM specialties WHERE id=? AND active=TRUE",Integer.class,specialtyId); if(durations.isEmpty())return ResponseEntity.notFound().build(); int duration=durations.get(0);
    String sql="SELECT ps.id,ps.start_at,ps.end_at,p.id, p.professional_code,l.id,l.name,s.name FROM professional_slots ps JOIN availability_blocks ab ON ab.id=ps.availability_block_id AND ab.active=TRUE JOIN professionals p ON p.id=ab.professional_id AND p.active=TRUE JOIN locations l ON l.id=ab.location_id JOIN specialties s ON s.id=? AND s.active=TRUE JOIN professional_specialties psp ON psp.professional_id=p.id AND psp.specialty_id=? AND psp.active=TRUE WHERE ab.available_date=? AND ps.appointment_id IS NULL";
    List<Object> args=new ArrayList<>(List.of(specialtyId,specialtyId,date)); if(locationId!=null){sql+=" AND l.id=?";args.add(locationId);} if(professionalId!=null){sql+=" AND p.id=?";args.add(professionalId);} sql+=" ORDER BY ps.start_at";
    // getObject(..., LocalDateTime.class) lee el DATETIME tal como esta almacenado. getTimestamp()
    // aplicaria la conversion entre serverTimezone del JDBC y la zona de la JVM, desplazando la hora.
    var rows=db.query(sql,(rs,n)->Map.of("slotId",rs.getLong(1),"startAt",rs.getObject(2,LocalDateTime.class),"endAt",rs.getObject(3,LocalDateTime.class),"professionalId",rs.getLong(4),"professionalCode",rs.getString(5),"locationId",rs.getLong(6),"locationName",rs.getString(7),"specialtyName",rs.getString(8)),args.toArray());
    if(duration==60){rows=rows.stream().filter(row->{var start=(LocalDateTime)row.get("startAt"); return db.queryForObject("SELECT COUNT(*) FROM professional_slots ps JOIN availability_blocks ab ON ab.id=ps.availability_block_id WHERE ab.available_date=? AND ab.professional_id=? AND ps.start_at=? AND ps.appointment_id IS NULL",Integer.class,date,row.get("professionalId"),start.plusMinutes(30))>0;}).toList();}
    return ResponseEntity.ok(Map.of("date",date,"durationMinutes",duration,"items",rows));
  }
}
