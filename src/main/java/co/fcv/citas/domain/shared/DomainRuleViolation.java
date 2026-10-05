package co.fcv.citas.domain.shared;

/**
 * Una regla de negocio incumplida.
 *
 * <p>No hereda de nada de Spring ni lleva un codigo HTTP: el dominio no sabe que existe HTTP. La
 * traduccion a {@code 400} o {@code 409} es decision del adaptador REST, que es quien conoce ese
 * vocabulario. Para que pueda decidirla sin leer el texto del mensaje, la violacion dice si el
 * problema es la peticion en si o el estado del sistema.
 */
public class DomainRuleViolation extends RuntimeException {

  /** Qué tipo de problema es, para que el adaptador elija el codigo sin interpretar el mensaje. */
  public enum Kind {
    /** La peticion no es valida en ningun caso: datos ausentes, valores fuera de rango. */
    INVALID_REQUEST,
    /** La peticion seria valida, pero el estado actual no la admite: franja tomada, cita cerrada. */
    CONFLICTING_STATE
  }

  private final Kind kind;

  private DomainRuleViolation(Kind kind, String message) {
    super(message);
    this.kind = kind;
  }

  public static DomainRuleViolation invalidRequest(String message) {
    return new DomainRuleViolation(Kind.INVALID_REQUEST, message);
  }

  public static DomainRuleViolation conflictingState(String message) {
    return new DomainRuleViolation(Kind.CONFLICTING_STATE, message);
  }

  public Kind kind() { return kind; }
}
