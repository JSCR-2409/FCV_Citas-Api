package co.fcv.citas.domain.appointment;

import java.time.Duration;
import java.time.LocalDateTime;

import co.fcv.citas.domain.shared.DomainRuleViolation;

/**
 * La ocupacion que exige una cita: desde cuando, hasta cuando y cuantas franjas de 30 minutos.
 *
 * <p>Codifica la discretizacion que el PRD da por supuesta y RN-05 hace explicita: la agenda se mide
 * en franjas de 30 minutos, de modo que una cita de 60 necesita <b>dos consecutivas</b>. Ese
 * «cuantas» era antes una division suelta, {@code duration / 30}, repetida en cada sitio que la
 * necesitaba.
 */
public record SlotPlan(LocalDateTime start, LocalDateTime end, int slotsNeeded) {

  /** La unidad de la agenda. Cambiarla es una decision de producto, no un detalle de consulta. */
  public static final int SLOT_MINUTES = 30;

  /** Duraciones admitidas por HU-012 CA-01. */
  private static final int[] ALLOWED_DURATIONS = {30, 60};

  /**
   * Plan de ocupacion para una cita que empieza en {@code start} y dura {@code durationMinutes}.
   *
   * @throws DomainRuleViolation si la duracion no es una de las admitidas, o si el inicio no cae en
   *     una frontera de franja. Lo segundo importa: una cita a las 09:15 no ocuparia una franja
   *     entera y dejaria quince minutos inalcanzables para cualquier otra.
   */
  public static SlotPlan of(LocalDateTime start, int durationMinutes) {
    if (start == null) throw DomainRuleViolation.invalidRequest("falta la hora de inicio");
    if (!isAllowedDuration(durationMinutes)) {
      throw DomainRuleViolation.invalidRequest("duración debe ser 30 o 60");
    }
    if (start.getMinute() % SLOT_MINUTES != 0 || start.getSecond() != 0 || start.getNano() != 0) {
      throw DomainRuleViolation.invalidRequest(
          "la cita debe empezar en una franja de " + SLOT_MINUTES + " minutos");
    }
    return new SlotPlan(start, start.plusMinutes(durationMinutes), durationMinutes / SLOT_MINUTES);
  }

  /**
   * Reconstruye el plan de una cita <b>ya existente</b>, sin validar.
   *
   * <p>La distincion no es un atajo: las invariantes se imponen al <b>crear</b>, no al leer. Una cita
   * guardada antes de que existiera una regla sigue siendo un hecho, y negarse a leerla porque hoy no
   * pasaria la validacion convertiria un dato historico en un error. {@link #of} es la puerta de
   * entrada y comprueba todo; esta es la de rehidratacion y no comprueba nada.
   */
  public static SlotPlan rehydrate(LocalDateTime start, LocalDateTime end) {
    if (start == null || end == null) {
      throw new IllegalArgumentException("una cita almacenada debe tener inicio y fin");
    }
    int minutes = (int) Duration.between(start, end).toMinutes();
    // El redondeo hacia arriba evita que una cita de 45 minutos quede con cero franjas.
    int slots = Math.max(1, (minutes + SLOT_MINUTES - 1) / SLOT_MINUTES);
    return new SlotPlan(start, end, slots);
  }

  public static boolean isAllowedDuration(int durationMinutes) {
    for (int allowed : ALLOWED_DURATIONS) if (allowed == durationMinutes) return true;
    return false;
  }

  public int durationMinutes() {
    return (int) Duration.between(start, end).toMinutes();
  }

  /** Una cita de 60 minutos necesita que la franja siguiente tambien este libre. */
  public boolean needsConsecutiveSlots() {
    return slotsNeeded > 1;
  }

  /**
   * Cierto si este plan y {@code other} comparten alguna franja. Se usa al reprogramar: si la nueva
   * hora se solapa con la original, liberar la original a ciegas borraria una franja que la cita
   * sigue necesitando.
   */
  public boolean overlaps(SlotPlan other) {
    return other != null && start.isBefore(other.end) && other.start.isBefore(end);
  }

  /** RN-06: ni citas ni bloques en el pasado. */
  public boolean startsBefore(LocalDateTime moment) {
    return start.isBefore(moment);
  }
}
