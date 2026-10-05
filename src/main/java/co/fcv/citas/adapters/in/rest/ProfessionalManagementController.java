package co.fcv.citas.adapters.in.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/professionals")
public class ProfessionalManagementController {
  private final JdbcTemplate db; private final PasswordEncoder encoder;
  public ProfessionalManagementController(JdbcTemplate db, PasswordEncoder encoder) { this.db=db; this.encoder=encoder; }

  @PostMapping @Transactional
  ResponseEntity<?> create(@Valid @RequestBody CreateRequest r) {
    try {
      if (db.queryForObject("SELECT COUNT(*) FROM users WHERE email=? OR (document_type=? AND document_number=?)",Integer.class,r.email().trim().toLowerCase(),r.documentType().trim(),r.documentNumber().trim())>0) return conflict("usuario ya registrado");
      if (db.queryForObject("SELECT COUNT(*) FROM professionals WHERE professional_code=? OR license_number=?",Integer.class,r.professionalCode(),r.licenseNumber())>0) return conflict("profesional ya registrado");
      var key=new GeneratedKeyHolder(); db.update(c->{var p=c.prepareStatement("INSERT INTO users(first_name,last_name,document_type,document_number,email,phone,password_hash,active,created_at) VALUES(?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)",new String[]{"id"}); p.setString(1,r.names());p.setString(2,r.surnames());p.setString(3,r.documentType());p.setString(4,r.documentNumber());p.setString(5,r.email().trim().toLowerCase());p.setString(6,r.phone());p.setString(7,encoder.encode(r.temporaryPassword()));p.setBoolean(8,true);return p;},key);
      long userId=key.getKey().longValue(); db.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE code='PROFESSIONAL'",userId);
      var professionalKey=new GeneratedKeyHolder(); db.update(c->{var p=c.prepareStatement("INSERT INTO professionals(user_id,professional_code,license_number,active) VALUES(?,?,?,TRUE)",new String[]{"id"});p.setLong(1,userId);p.setString(2,r.professionalCode());p.setString(3,r.licenseNumber());return p;},professionalKey);
      return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id",professionalKey.getKey().longValue(),"professionalCode",r.professionalCode(),"licenseNumber",r.licenseNumber(),"email",r.email().trim().toLowerCase(),"active",true));
    } catch (DuplicateKeyException e) { return conflict("registro duplicado"); }
  }

  /**
   * Listado para administrar: sin el, el ADMIN no puede asignar especialidades ni sedes porque no
   * sabe que profesionales existen. Incluye los inactivos, que son precisamente los que hay que
   * poder reactivar. Se agrupan las asignaciones en memoria para no consultar una vez por fila.
   */
  @GetMapping
  ResponseEntity<?> list() {
    var specialtiesByProfessional = new HashMap<Long, List<Map<String,Object>>>();
    db.query("SELECT ps.professional_id,s.id,s.name,ps.is_primary,s.active FROM professional_specialties ps"
        + " JOIN specialties s ON s.id=ps.specialty_id WHERE ps.active=TRUE ORDER BY s.name", rs -> {
          specialtiesByProfessional.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>())
              .add(Map.of("id", rs.getLong(2), "name", rs.getString(3),
                  "primary", rs.getBoolean(4), "active", rs.getBoolean(5)));
        });

    var locationsByProfessional = new HashMap<Long, List<Map<String,Object>>>();
    db.query("SELECT pl.professional_id,l.id,l.name FROM professional_locations pl"
        + " JOIN locations l ON l.id=pl.location_id ORDER BY l.name", rs -> {
          locationsByProfessional.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>())
              .add(Map.of("id", rs.getLong(2), "name", rs.getString(3)));
        });

    var rows = db.query("SELECT p.id,p.professional_code,p.license_number,p.active,"
        + "u.id,u.first_name,u.last_name,u.email"
        + " FROM professionals p JOIN users u ON u.id=p.user_id ORDER BY u.last_name,u.first_name",
        (rs, n) -> {
          long professionalId = rs.getLong(1);
          var item = new LinkedHashMap<String,Object>();
          item.put("id", professionalId);
          item.put("professionalCode", rs.getString(2));
          item.put("licenseNumber", rs.getString(3));
          item.put("active", rs.getBoolean(4));
          item.put("userId", rs.getLong(5));
          item.put("name", rs.getString(6) + " " + rs.getString(7));
          item.put("email", rs.getString(8));
          item.put("specialties", specialtiesByProfessional.getOrDefault(professionalId, List.of()));
          item.put("locations", locationsByProfessional.getOrDefault(professionalId, List.of()));
          return item;
        });
    return ResponseEntity.ok(rows);
  }

  @PutMapping("/{id}/specialties") @Transactional
  ResponseEntity<?> specialties(@PathVariable long id,@RequestBody AssignmentRequest r) { if(!exists("professionals",id)) return ResponseEntity.notFound().build(); if(r.assignments()==null||r.assignments().isEmpty()||r.assignments().stream().filter(Assignment::primary).count()!=1||r.assignments().stream().map(Assignment::id).distinct().count()!=r.assignments().size()) return ResponseEntity.badRequest().body(Map.of("message","asignaciones inválidas")); for(var a:r.assignments()) if(db.queryForObject("SELECT COUNT(*) FROM specialties WHERE id=? AND active=TRUE",Integer.class,a.id())==0) return ResponseEntity.badRequest().body(Map.of("message","especialidad inválida o inactiva")); db.update("DELETE FROM professional_specialties WHERE professional_id=?",id); for(var a:r.assignments()) db.update("INSERT INTO professional_specialties(professional_id,specialty_id,is_primary,active) VALUES(?,?,?,TRUE)",id,a.id(),a.primary()); return ResponseEntity.ok(Map.of("professionalId",id,"assignments",r.assignments())); }

  @PutMapping("/{id}/locations") @Transactional
  ResponseEntity<?> locations(@PathVariable long id,@RequestBody IdsRequest r) { if(!exists("professionals",id)) return ResponseEntity.notFound().build(); if(r.ids()==null||r.ids().isEmpty()||r.ids().stream().distinct().count()!=r.ids().size()) return ResponseEntity.badRequest().body(Map.of("message","sedes inválidas")); for(Long location:r.ids()) if(!exists("locations",location)) return ResponseEntity.badRequest().body(Map.of("message","sede inválida")); db.update("DELETE FROM professional_locations WHERE professional_id=?",id); for(Long location:r.ids()) db.update("INSERT INTO professional_locations(professional_id,location_id) VALUES(?,?)",id,location); return ResponseEntity.ok(Map.of("professionalId",id,"locationIds",r.ids())); }

  @PatchMapping("/{id}/active") ResponseEntity<?> active(@PathVariable long id,@RequestBody ActiveRequest r) { int n=db.update("UPDATE professionals SET active=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",r.active(),id); return n==0?ResponseEntity.notFound().build():ResponseEntity.ok(Map.of("id",id,"active",r.active())); }
  private boolean exists(String table,long id){ return db.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE id=?",Integer.class,id)>0; }
  private ResponseEntity<Map<String,String>> conflict(String message){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message",message));}
  record CreateRequest(@NotBlank String names,@NotBlank String surnames,@NotBlank String documentType,@NotBlank String documentNumber,@Email @NotBlank String email,@NotBlank String phone,@NotBlank @Size(min=12) String temporaryPassword,@NotBlank String professionalCode,@NotBlank String licenseNumber){}
  record Assignment(long id,boolean primary){}
  record AssignmentRequest(List<Assignment> assignments){}
  record IdsRequest(List<Long> ids){}
  record ActiveRequest(boolean active){}
}
