package co.fcv.citas.appointment;
import java.util.Map; import org.springframework.http.*; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.bind.annotation.*; import org.springframework.security.core.Authentication;
@RestController @RequestMapping("/api/v1/me/appointments") public class PatientAppointmentController { private final JdbcTemplate db; private final AppointmentStatusLog history; private final co.fcv.citas.integration.StatusChangeNotifier notifier; public PatientAppointmentController(JdbcTemplate db,AppointmentStatusLog h,co.fcv.citas.integration.StatusChangeNotifier n){this.db=db;this.history=h;this.notifier=n;}
 @PatchMapping("/{id}/cancel") @Transactional ResponseEntity<?> cancel(@PathVariable long id, Authentication a){long user=Long.parseLong(a.getName()); int n=db.update("UPDATE appointments SET status_id=(SELECT id FROM appointment_statuses WHERE code='CANCELLED'),updated_at=CURRENT_TIMESTAMP WHERE id=? AND patient_user_id=? AND scheduled_start_at>NOW() AND status_id IN (SELECT id FROM appointment_statuses WHERE code IN ('APPROVED','REQUESTED'))",id,user); if(n==0)return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message","La cita no puede cancelarse")); db.update("UPDATE professional_slots SET appointment_id=NULL WHERE appointment_id=?",id); // Cancelar la cita cierra tambien cualquier reprogramacion pendiente: el ADMIN ya no tiene que decidir sobre una cita que no existe.
 db.update("UPDATE reschedule_requests SET status_id=(SELECT id FROM reschedule_request_statuses WHERE code='CANCELLED') WHERE appointment_id=? AND status_id=(SELECT id FROM reschedule_request_statuses WHERE code='PENDING')",id);
 // HU-023 CA-03 y HU-031: la cancelacion queda auditada con su actor. La fuente es USER porque la
 // pide el titular de la cita, no la operacion.
 history.record(id,"CANCELLED",user,AppointmentStatusLog.SOURCE_USER,null);
 // HU-033: la cancelacion es uno de los eventos de WF-002. El aviso es asincrono y no propaga
 // errores: que n8n este caido no puede impedir que alguien cancele su cita.
 notifier.publish("APPOINTMENT_CANCELLED",id,"CANCELLED",null); return ResponseEntity.ok(Map.of("id",id,"status","CANCELLED")); }
}
