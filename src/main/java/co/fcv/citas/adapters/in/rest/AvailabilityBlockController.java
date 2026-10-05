package co.fcv.citas.adapters.in.rest;

import java.time.*;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/professional/availability-blocks")
public class AvailabilityBlockController {
  private final JdbcTemplate db;
  public AvailabilityBlockController(JdbcTemplate db) { this.db = db; }

  @PostMapping @Transactional
  ResponseEntity<?> create(@RequestBody Request r, Authentication auth) {
    if (r == null || r.availableDate() == null || r.startTime() == null || r.endTime() == null || r.locationId() == null) return bad("datos incompletos");
    if (!r.availableDate().isAfter(LocalDate.now()) || !r.endTime().isAfter(r.startTime()) || (r.startTime().toSecondOfDay() % 1800 != 0) || (r.endTime().toSecondOfDay() % 1800 != 0)) return bad("fecha u horario inválido");
    long userId = Long.parseLong(auth.getName());
    var professionalIds = db.query("SELECT p.id FROM professionals p WHERE p.user_id=? AND p.active=TRUE", (rs, row) -> rs.getLong(1), userId);
    Long professionalId = professionalIds.isEmpty() ? null : professionalIds.get(0);
    if (professionalId == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message","profesional inactivo o inexistente"));
    if (db.queryForObject("SELECT COUNT(*) FROM professional_locations WHERE professional_id=? AND location_id=?",Integer.class,professionalId,r.locationId())==0) return bad("sede no asignada");
    if (db.queryForObject("SELECT COUNT(*) FROM availability_blocks WHERE professional_id=? AND location_id=? AND available_date=? AND active=TRUE AND start_time < ? AND end_time > ?",Integer.class,professionalId,r.locationId(),r.availableDate(),r.endTime(),r.startTime())>0) return conflict("bloque solapado");
    var key = new org.springframework.jdbc.support.GeneratedKeyHolder();
    db.update(c->{var p=c.prepareStatement("INSERT INTO availability_blocks(professional_id,location_id,available_date,start_time,end_time,active) VALUES(?,?,?,?,?,TRUE)",new String[]{"id"});p.setLong(1,professionalId);p.setLong(2,r.locationId());p.setObject(3,r.availableDate());p.setObject(4,r.startTime());p.setObject(5,r.endTime());return p;},key);
    long blockId=key.getKey().longValue(); LocalDateTime cursor=LocalDateTime.of(r.availableDate(),r.startTime()), end=LocalDateTime.of(r.availableDate(),r.endTime());
    while(cursor.isBefore(end)){ LocalDateTime next=cursor.plusMinutes(30); db.update("INSERT INTO professional_slots(availability_block_id,start_at,end_at) VALUES(?,?,?)",blockId,cursor,next); cursor=next; }
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id",blockId,"professionalId",professionalId,"locationId",r.locationId(),"availableDate",r.availableDate(),"startTime",r.startTime(),"endTime",r.endTime(),"slotMinutes",30));
  }
  @GetMapping
  ResponseEntity<?> list(Authentication auth, @RequestParam(required=false) LocalDate from, @RequestParam(required=false) LocalDate to) {
    // El rango por defecto se resuelve en Java: DATE_ADD(... INTERVAL ...) es sintaxis propia
    // de MySQL y no se puede verificar en las pruebas.
    long userId=Long.parseLong(auth.getName()); LocalDate desde=from!=null?from:LocalDate.now(); LocalDate hasta=to!=null?to:LocalDate.now().plusDays(30); var rows=db.query("SELECT ab.id,ab.location_id,ab.available_date,ab.start_time,ab.end_time,ab.active,l.name FROM availability_blocks ab JOIN professionals p ON p.id=ab.professional_id JOIN locations l ON l.id=ab.location_id WHERE p.user_id=? AND ab.available_date BETWEEN ? AND ? ORDER BY ab.available_date,ab.start_time",(rs,n)->Map.of("id",rs.getLong(1),"locationId",rs.getLong(2),"availableDate",rs.getObject(3,LocalDate.class),"startTime",rs.getObject(4,LocalTime.class),"endTime",rs.getObject(5,LocalTime.class),"active",rs.getBoolean(6),"locationName",rs.getString(7)),userId,desde,hasta); return ResponseEntity.ok(rows);
  }
  @PutMapping("/{id}") @Transactional
  ResponseEntity<?> update(@PathVariable long id,@RequestBody Request r,Authentication auth){ long userId=Long.parseLong(auth.getName()); var owner=db.query("SELECT ab.available_date FROM availability_blocks ab JOIN professionals p ON p.id=ab.professional_id WHERE ab.id=? AND p.user_id=?",(rs,n)->rs.getObject(1,LocalDate.class),id,userId); if(owner.isEmpty()) return ResponseEntity.notFound().build(); if(!owner.get(0).isAfter(LocalDate.now())) return bad("bloque pasado"); if(!r.availableDate().isAfter(LocalDate.now())||!r.endTime().isAfter(r.startTime())) return bad("fecha u horario inválido"); if(db.queryForObject("SELECT COUNT(*) FROM professional_slots WHERE availability_block_id=? AND appointment_id IS NOT NULL",Integer.class,id)>0)return conflict("bloque comprometido"); db.update("UPDATE availability_blocks SET location_id=?,available_date=?,start_time=?,end_time=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",r.locationId(),r.availableDate(),r.startTime(),r.endTime(),id); db.update("DELETE FROM professional_slots WHERE availability_block_id=?",id); LocalDateTime c=LocalDateTime.of(r.availableDate(),r.startTime()),e=LocalDateTime.of(r.availableDate(),r.endTime()); while(c.isBefore(e)){var n=c.plusMinutes(30);db.update("INSERT INTO professional_slots(availability_block_id,start_at,end_at) VALUES(?,?,?)",id,c,n);c=n;} return ResponseEntity.ok(Map.of("id",id,"updated",true)); }
  @DeleteMapping("/{id}") @Transactional
  ResponseEntity<?> delete(@PathVariable long id,Authentication auth){ long userId=Long.parseLong(auth.getName()); var owner=db.query("SELECT ab.available_date FROM availability_blocks ab JOIN professionals p ON p.id=ab.professional_id WHERE ab.id=? AND p.user_id=?",(rs,n)->rs.getObject(1,LocalDate.class),id,userId); if(owner.isEmpty())return ResponseEntity.notFound().build(); if(!owner.get(0).isAfter(LocalDate.now()))return bad("bloque pasado"); if(db.queryForObject("SELECT COUNT(*) FROM professional_slots WHERE availability_block_id=? AND appointment_id IS NOT NULL",Integer.class,id)>0)return conflict("bloque comprometido"); db.update("DELETE FROM professional_slots WHERE availability_block_id=?",id); db.update("UPDATE availability_blocks SET active=FALSE,updated_at=CURRENT_TIMESTAMP WHERE id=?",id); return ResponseEntity.noContent().build(); }
  private ResponseEntity<Map<String,String>> bad(String m){return ResponseEntity.badRequest().body(Map.of("message",m));}
  private ResponseEntity<Map<String,String>> conflict(String m){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message",m));}
  record Request(Long locationId, LocalDate availableDate, LocalTime startTime, LocalTime endTime) {}
}
