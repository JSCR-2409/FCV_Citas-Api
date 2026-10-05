package co.fcv.citas.domain.appointment;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import co.fcv.citas.domain.shared.DomainRuleViolation;

/**
 * Reglas de cita, ejercidas <b>sin Spring, sin base de datos y sin HTTP</b>.
 *
 * <p>Esta clase es la evidencia de que «dominio/aplicación independientes de adaptadores» es cierto:
 * no lleva ni una anotación de framework y corre en milisegundos. Si alguien colase una dependencia
 * de Spring o de JPA en el dominio, esta prueba dejaría de compilar o de arrancar.
 *
 * <p>Las mismas reglas están cubiertas end-to-end en las pruebas de REST. Lo que añade este nivel es
 * poder leerlas juntas y ejercer los casos límite sin montar un escenario completo.
 */
class AppointmentDomainTest {

  private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 10, 12, 0);
  private static final long PACIENTE = 7L;
  private static final long PROFESIONAL = 3L;

  private Appointment cita(AppointmentStatus estado, LocalDateTime inicio, int minutos) {
    return new Appointment(1L, PACIENTE, PROFESIONAL, 1L, 1L,
        SlotPlan.of(inicio, minutos), estado, null);
  }

  // --- SlotPlan: la discretización de la agenda ----------------------------------------------

  @Test
  void treintaMinutosOcupaUnaFranjaYSesentaDos() {
    Assertions.assertEquals(1, SlotPlan.of(AHORA, 30).slotsNeeded());
    Assertions.assertEquals(2, SlotPlan.of(AHORA, 60).slotsNeeded());
    Assertions.assertTrue(SlotPlan.of(AHORA, 60).needsConsecutiveSlots());
    Assertions.assertFalse(SlotPlan.of(AHORA, 30).needsConsecutiveSlots());
  }

  @Test
  void unaDuracionQueNoSeaTreintaOSesentaSeRechaza() {
    for (int minutos : new int[] {0, 15, 45, 90, -30}) {
      var error = Assertions.assertThrows(DomainRuleViolation.class, () -> SlotPlan.of(AHORA, minutos));
      Assertions.assertEquals(DomainRuleViolation.Kind.INVALID_REQUEST, error.kind());
    }
  }

  /**
   * Una cita a las 09:15 no ocuparía una franja entera y dejaría quince minutos inalcanzables para
   * cualquier otra reserva.
   */
  @Test
  void laCitaDebeEmpezarEnUnaFronteraDeFranja() {
    for (var inicio : new LocalDateTime[] {
        LocalDateTime.of(2026, 10, 10, 9, 15),
        LocalDateTime.of(2026, 10, 10, 9, 1),
        LocalDateTime.of(2026, 10, 10, 9, 0, 30)}) {
      Assertions.assertThrows(DomainRuleViolation.class, () -> SlotPlan.of(inicio, 30));
    }
    Assertions.assertDoesNotThrow(() -> SlotPlan.of(LocalDateTime.of(2026, 10, 10, 9, 30), 30));
  }

  /** Importa al reprogramar: liberar la franja original a ciegas borraría una que sigue en uso. */
  @Test
  void elSolapamientoSeDetectaEnLosDosSentidos() {
    var original = SlotPlan.of(LocalDateTime.of(2026, 10, 10, 9, 0), 60);
    var mediaHoraDespues = SlotPlan.of(LocalDateTime.of(2026, 10, 10, 9, 30), 60);
    var siguienteHora = SlotPlan.of(LocalDateTime.of(2026, 10, 10, 10, 0), 60);

    Assertions.assertTrue(original.overlaps(mediaHoraDespues));
    Assertions.assertTrue(mediaHoraDespues.overlaps(original));
    // Contiguas no es solapado: la de las 10:00 empieza justo cuando la de las 09:00 termina.
    Assertions.assertFalse(original.overlaps(siguienteHora));
    Assertions.assertFalse(original.overlaps(null));
  }

  // --- SpecialtyKind: RN-02 y RN-03 ----------------------------------------------------------

  @Test
  void unaGeneralNaceAprobadaYUnaEspecializadaPendiente() {
    Assertions.assertEquals(AppointmentStatus.APPROVED, SpecialtyKind.GENERAL.initialStatus());
    Assertions.assertEquals(AppointmentStatus.REQUESTED, SpecialtyKind.SPECIALIZED.initialStatus());
    Assertions.assertEquals(SpecialtyKind.GENERAL, SpecialtyKind.of(true));
    Assertions.assertEquals(SpecialtyKind.SPECIALIZED, SpecialtyKind.of(false));
  }

  // --- AppointmentStatus: qué es terminal y qué retiene franja --------------------------------

  @Test
  void losCuatroEstadosTerminalesSonLosQueYaOcurrieronOSeDeshicieron() {
    for (var terminal : new AppointmentStatus[] {AppointmentStatus.REJECTED,
        AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW}) {
      Assertions.assertTrue(terminal.isTerminal(), terminal + " debe ser terminal");
    }
    Assertions.assertFalse(AppointmentStatus.REQUESTED.isTerminal());
    Assertions.assertFalse(AppointmentStatus.APPROVED.isTerminal());
  }

  /** COMPLETED y NO_SHOW son terminales pero conservan la franja: la atención consumió el horario. */
  @Test
  void unaAtencionPrestadaSigueOcupandoSuFranja() {
    Assertions.assertTrue(AppointmentStatus.COMPLETED.holdsSlots());
    Assertions.assertTrue(AppointmentStatus.NO_SHOW.holdsSlots());
    Assertions.assertFalse(AppointmentStatus.CANCELLED.holdsSlots());
    Assertions.assertFalse(AppointmentStatus.REJECTED.holdsSlots());
  }

  @Test
  void unEstadoDesconocidoFallaEnLugarDeDevolverNulo() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> AppointmentStatus.of("INVENTADO"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> AppointmentStatus.of(null));
    Assertions.assertEquals(AppointmentStatus.APPROVED, AppointmentStatus.of(" approved "));
  }

  // --- HU-023: cancelación -------------------------------------------------------------------

  @Test
  void hu023_unaCitaPropiaFuturaYVigenteSeCancela() {
    var cita = cita(AppointmentStatus.APPROVED, AHORA.plusDays(1), 30);
    cita.cancelBy(PACIENTE, AHORA);
    Assertions.assertEquals(AppointmentStatus.CANCELLED, cita.status());
  }

  @Test
  void hu023_unaSolicitudPendienteTambienSeCancela() {
    var cita = cita(AppointmentStatus.REQUESTED, AHORA.plusDays(1), 30);
    cita.cancelBy(PACIENTE, AHORA);
    Assertions.assertEquals(AppointmentStatus.CANCELLED, cita.status());
  }

  @Test
  void hu023_nadieCancelaLaCitaDeOtro() {
    var cita = cita(AppointmentStatus.APPROVED, AHORA.plusDays(1), 30);
    Assertions.assertThrows(DomainRuleViolation.class, () -> cita.cancelBy(99L, AHORA));
    Assertions.assertEquals(AppointmentStatus.APPROVED, cita.status());
  }

  @Test
  void hu023_unaCitaPasadaNoSeCancela() {
    var cita = cita(AppointmentStatus.APPROVED, AHORA.minusHours(2), 30);
    Assertions.assertThrows(DomainRuleViolation.class, () -> cita.cancelBy(PACIENTE, AHORA));
    Assertions.assertEquals(AppointmentStatus.APPROVED, cita.status());
  }

  @Test
  void hu023_losCuatroEstadosTerminalesNoSeCancelan() {
    for (var terminal : new AppointmentStatus[] {AppointmentStatus.CANCELLED,
        AppointmentStatus.REJECTED, AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW}) {
      var cita = cita(terminal, AHORA.plusDays(1), 30);
      Assertions.assertThrows(DomainRuleViolation.class, () -> cita.cancelBy(PACIENTE, AHORA),
          terminal + " no debe poder cancelarse");
      Assertions.assertEquals(terminal, cita.status());
    }
  }

  // --- HU-028: decisión administrativa -------------------------------------------------------

  @Test
  void hu028_aprobarUnaSolicitudPendienteLaDejaAprobada() {
    var cita = cita(AppointmentStatus.REQUESTED, AHORA.plusDays(1), 30);
    cita.decide(AppointmentStatus.APPROVED, null);
    Assertions.assertEquals(AppointmentStatus.APPROVED, cita.status());
  }

  /** RN-04: el motivo del rechazo es una condición del dominio, no una validación de formulario. */
  @Test
  void hu028_rechazarSinMotivoEsImposible() {
    for (String sinMotivo : new String[] {null, "", "   "}) {
      var cita = cita(AppointmentStatus.REQUESTED, AHORA.plusDays(1), 30);
      var error = Assertions.assertThrows(DomainRuleViolation.class,
          () -> cita.decide(AppointmentStatus.REJECTED, sinMotivo));
      Assertions.assertEquals(DomainRuleViolation.Kind.INVALID_REQUEST, error.kind());
      Assertions.assertEquals(AppointmentStatus.REQUESTED, cita.status());
    }
  }

  @Test
  void hu028_rechazarConMotivoLoConserva() {
    var cita = cita(AppointmentStatus.REQUESTED, AHORA.plusDays(1), 30);
    cita.decide(AppointmentStatus.REJECTED, "El profesional no atiende ese día");
    Assertions.assertEquals(AppointmentStatus.REJECTED, cita.status());
    Assertions.assertEquals("El profesional no atiende ese día", cita.reason());
  }

  /** Sin esta guarda, una cita ya aprobada podía redecidirse y perder su franja. */
  @Test
  void hu028_unaSolicitudYaResueltaNoSeRedecide() {
    for (var resuelta : new AppointmentStatus[] {AppointmentStatus.APPROVED,
        AppointmentStatus.REJECTED, AppointmentStatus.CANCELLED}) {
      var cita = cita(resuelta, AHORA.plusDays(1), 30);
      var error = Assertions.assertThrows(DomainRuleViolation.class,
          () -> cita.decide(AppointmentStatus.REJECTED, "un motivo"));
      Assertions.assertEquals(DomainRuleViolation.Kind.CONFLICTING_STATE, error.kind());
      Assertions.assertEquals(resuelta, cita.status());
    }
  }

  @Test
  void hu028_unaDecisionQueNoSeaAprobarNiRechazarSeRechaza() {
    var cita = cita(AppointmentStatus.REQUESTED, AHORA.plusDays(1), 30);
    Assertions.assertThrows(DomainRuleViolation.class,
        () -> cita.decide(AppointmentStatus.COMPLETED, "x"));
  }

  // --- HU-026: cierre de atención ------------------------------------------------------------

  @Test
  void hu026_unaCitaVigenteYaComenzadaSeCierra() {
    for (var resultado : new AppointmentStatus[] {AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW}) {
      var cita = cita(AppointmentStatus.APPROVED, AHORA.minusHours(1), 30);
      cita.closeAttention(resultado, PROFESIONAL, AHORA);
      Assertions.assertEquals(resultado, cita.status());
    }
  }

  @Test
  void hu026_unaCitaFuturaNoSeCierra() {
    var cita = cita(AppointmentStatus.APPROVED, AHORA.plusDays(1), 30);
    Assertions.assertThrows(DomainRuleViolation.class,
        () -> cita.closeAttention(AppointmentStatus.COMPLETED, PROFESIONAL, AHORA));
    Assertions.assertEquals(AppointmentStatus.APPROVED, cita.status());
  }

  @Test
  void hu026_otroProfesionalNoCierraLaAtencion() {
    var cita = cita(AppointmentStatus.APPROVED, AHORA.minusHours(1), 30);
    Assertions.assertThrows(DomainRuleViolation.class,
        () -> cita.closeAttention(AppointmentStatus.COMPLETED, 99L, AHORA));
    Assertions.assertEquals(AppointmentStatus.APPROVED, cita.status());
  }

  @Test
  void hu026_cerrarDosVecesEsImposible() {
    var cita = cita(AppointmentStatus.APPROVED, AHORA.minusHours(1), 30);
    cita.closeAttention(AppointmentStatus.COMPLETED, PROFESIONAL, AHORA);
    Assertions.assertThrows(DomainRuleViolation.class,
        () -> cita.closeAttention(AppointmentStatus.NO_SHOW, PROFESIONAL, AHORA));
    Assertions.assertEquals(AppointmentStatus.COMPLETED, cita.status());
  }

  @Test
  void hu026_elResultadoDebeSerAtendidaONoAsistio() {
    var cita = cita(AppointmentStatus.APPROVED, AHORA.minusHours(1), 30);
    for (var invalido : new AppointmentStatus[] {AppointmentStatus.CANCELLED,
        AppointmentStatus.APPROVED, AppointmentStatus.REJECTED, null}) {
      var error = Assertions.assertThrows(DomainRuleViolation.class,
          () -> cita.closeAttention(invalido, PROFESIONAL, AHORA));
      Assertions.assertEquals(DomainRuleViolation.Kind.INVALID_REQUEST, error.kind());
    }
  }

  // --- HU-024: reprogramación ----------------------------------------------------------------

  @Test
  void hu024_soloUnaCitaAprobadaYFuturaSeReprograma() {
    var reprogramable = cita(AppointmentStatus.APPROVED, AHORA.plusDays(1), 30);
    Assertions.assertDoesNotThrow(() -> reprogramable.assertReschedulable(PACIENTE, AHORA));

    var pendiente = cita(AppointmentStatus.REQUESTED, AHORA.plusDays(1), 30);
    Assertions.assertThrows(DomainRuleViolation.class, () -> pendiente.assertReschedulable(PACIENTE, AHORA));

    var pasada = cita(AppointmentStatus.APPROVED, AHORA.minusDays(1), 30);
    Assertions.assertThrows(DomainRuleViolation.class, () -> pasada.assertReschedulable(PACIENTE, AHORA));

    var ajena = cita(AppointmentStatus.APPROVED, AHORA.plusDays(1), 30);
    Assertions.assertThrows(DomainRuleViolation.class, () -> ajena.assertReschedulable(99L, AHORA));
  }

  @Test
  void hu024_moverLaCitaDevuelveLaFranjaAnteriorParaPoderLiberarla() {
    var cita = cita(AppointmentStatus.APPROVED, AHORA.plusDays(1).withHour(9).withMinute(0), 60);
    var anterior = cita.slots();
    var nueva = SlotPlan.of(AHORA.plusDays(2).withHour(14).withMinute(0), 60);

    var devuelta = cita.moveTo(nueva);

    Assertions.assertEquals(anterior, devuelta);
    Assertions.assertEquals(nueva, cita.slots());
  }
}
