package co.fcv.citas.domain.appointment;

/**
 * Si una especialidad se atiende de inmediato o exige decision administrativa.
 *
 * <p>Es la distincion de la que dependen RN-02 y RN-03, y la razon de que existan dos endpoints de
 * reserva. Antes vivia como un {@code boolean general} que viajaba de parametro en parametro, y en el
 * cliente llego a deducirse de que el identificador fuera {@code 1}.
 */
public enum SpecialtyKind {

  /** RN-02: la cita se crea ya aprobada. */
  GENERAL,
  /** RN-03: la cita nace pendiente y retiene la franja hasta que el ADMIN decida. */
  SPECIALIZED;

  public static SpecialtyKind of(boolean general) {
    return general ? GENERAL : SPECIALIZED;
  }

  public boolean isGeneral() {
    return this == GENERAL;
  }

  /** El estado con el que nace una cita de esta clase de especialidad. */
  public AppointmentStatus initialStatus() {
    return this == GENERAL ? AppointmentStatus.APPROVED : AppointmentStatus.REQUESTED;
  }
}
