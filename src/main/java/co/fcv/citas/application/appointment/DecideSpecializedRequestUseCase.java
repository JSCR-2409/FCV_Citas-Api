package co.fcv.citas.application.appointment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.fcv.citas.application.appointment.port.out.AppointmentRepositoryPort;
import co.fcv.citas.application.appointment.port.out.SlotReservationPort;
import co.fcv.citas.application.appointment.port.out.StatusHistoryPort;
import co.fcv.citas.domain.appointment.Appointment;
import co.fcv.citas.domain.appointment.AppointmentStatus;
import co.fcv.citas.domain.appointment.ChangeSource;
import co.fcv.citas.domain.shared.DomainRuleViolation;

/**
 * HU-028 — El ADMIN aprueba o rechaza una cita especializada.
 *
 * <p>La guarda que importa la impone {@link Appointment#decide}: solo una solicitud que sigue
 * pendiente puede decidirse. Sin ella, una cita ya aprobada podia redecidirse y perder su franja, que
 * es el defecto que se reprodujo en vivo durante S3.
 */
@Service
public class DecideSpecializedRequestUseCase {

  private final AppointmentRepositoryPort appointments;
  private final SlotReservationPort slots;
  private final StatusHistoryPort history;

  public DecideSpecializedRequestUseCase(AppointmentRepositoryPort appointments,
                                         SlotReservationPort slots, StatusHistoryPort history) {
    this.appointments = appointments;
    this.slots = slots;
    this.history = history;
  }

  @Transactional
  public Appointment decide(long appointmentId, AppointmentStatus decision, String reason,
                            long adminUserId) {
    var appointment = appointments.findById(appointmentId)
        .orElseThrow(() -> new NotFound(appointmentId));

    // Valida la decision, exige motivo en el rechazo (RN-04) y comprueba que siga pendiente.
    appointment.decide(decision, reason);

    appointments.updateStatus(appointment, adminUserId);
    // Rechazar libera la retencion; aprobar la conserva, que es justo lo contrario.
    if (decision == AppointmentStatus.REJECTED) slots.releaseAll(appointmentId);
    history.record(appointmentId, decision, adminUserId, ChangeSource.ADMIN, reason);

    return appointment;
  }

  /**
   * Una cita inexistente es un {@code 404} y no un conflicto de estado, de modo que necesita su propio
   * tipo: {@link DomainRuleViolation} solo distingue peticion invalida de estado incompatible.
   */
  public static class NotFound extends RuntimeException {
    public NotFound(long appointmentId) { super("la cita " + appointmentId + " no existe"); }
  }
}
