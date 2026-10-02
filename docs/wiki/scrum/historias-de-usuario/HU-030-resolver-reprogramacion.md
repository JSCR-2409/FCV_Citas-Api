---
id: HU-030
tipo: historia-de-usuario
titulo: "Resolver reprogramación"
estado: Completada
epica: "[[EP-007-operacion-administrativa-y-auditoria]]"
esfuerzo: Muy alto
sprint_sugerido: "Incremento V — Operación clínica simulada"
dependencias: ["[[HU-029-consultar-bandeja-reprogramaciones]]"]
relacionadas: ["[[HU-023-cancelar-cita]]", "[[HU-031-auditar-cambios-de-estado]]"]
---

# HU-030 — Resolver reprogramación

## Historia de usuario
**COMO** ADMIN **QUIERO** aprobar o rechazar una reprogramación PENDING **PARA** mover una cita solo cuando corresponda y mantenerla si rechazo la propuesta.

## Alcance
- Aprobar: libera slots antiguos, asigna nuevos y actualiza cita; rechazar: libera propuesta y conserva original; motivo cuando corresponda; auditoría.

## Fuera de alcance
- Cambiar profesional/especialidad, destruir original al rechazar o impedir que USER cancele tras rechazo.

## Reglas de negocio
- Original se conserva hasta decisión; aprobación mueve; rechazo conserva original/libera propuesta; USER puede conservar o cancelar tras rechazo.

## Dependencias y relaciones
- Épica: [[EP-007-operacion-administrativa-y-auditoria]]
- Dependencias: [[HU-029-consultar-bandeja-reprogramaciones]]
- Relacionadas: [[HU-023-cancelar-cita]], [[HU-031-auditar-cambios-de-estado]]

## Esfuerzo
**Nivel:** Muy alto. Es una transición coordinada de dos franjas y dos objetos con riesgo transaccional; se mantiene separada de la solicitud.

## Tareas de desarrollo
- [x] **T-01 — Precisar estados, motivo y efectos de decisión.** Dificultad: Alto. Registrar reglas faltantes explícitamente.
- [x] **T-02 — Implementar aprobación/rechazo atómico con slots e historial.** Dificultad: Alto. Evitar pérdida/doble reserva.
- [x] **T-03 — Integrar decisión ADMIN, actualización USER y pruebas.** Dificultad: Alto. Comprobar las dos ramas de resultado.

## Criterios de aceptación
### CA-01 — Aprobación mueve la cita
Dado una reprogramación PENDING válida, cuando ADMIN aprueba, entonces se liberan slots antiguos, se asignan nuevos y la cita queda actualizada.
### CA-02 — Rechazo preserva original
Dado una reprogramación PENDING, cuando ADMIN rechaza con el motivo aplicable, entonces se libera propuesta, se mantiene la cita original y USER puede conservarla o cancelarla.
### CA-03 — Consistencia ante conflicto
Dado una solicitud ya resuelta o una propuesta que no puede confirmarse según regla aprobada, cuando se intenta decidir, entonces no se producen cambios parciales y se informa resultado conforme a contrato.

## Definition of Done
- [x] CA-01 a CA-03 tienen pruebas transaccionales, concurrencia, estados, RBAC y frontend aplicable.
- [x] Las actualizaciones de cita/slots/solicitud/auditoría son atómicas y cuentan con Flyway/índices si aplica.
- [x] Contrato y UI comunican aprobación, rechazo, motivo y opción posterior de cancelación sin BFF.
- [x] La trazabilidad Scrum está actualizada.

## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumplido | ReschedulingTest.hu030_ca01_approvalMovesTheAppointmentAndFreesTheOldSlot y hu030_ca01_overlappingProposalDoesNotFreeASlotStillNeeded | Verificado el 2026-10-02 |
| CA-02 | Cumplido | ReschedulingTest.hu030_ca02_rejectionKeepsTheOriginalAndFreesTheProposal y hu030_ca02_afterRejectionThePatientKeepsOrCancels | Verificado el 2026-10-02 |
| CA-03 | Cumplido | ReschedulingTest.hu030_ca03_anAlreadyResolvedRequestCannotBeDecidedAgain, hu030_ca03_rejectionWithoutReasonChangesNothing, hu030_ca03_aProposalThatIsNoLongerHeldCannotBeApproved y hu030_ca03_unknownRequestReturnsNotFound | Verificado el 2026-10-02 |
| DoD | Cumplida: pruebas REST, liberacion selectiva con solape y UI de aprobacion y rechazo con motivo obligatorio | Suite de 83 pruebas de backend en verde | Revisado el 2026-10-02 |

## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-02 — Implementacion verificada contra la suite de pruebas y el entorno MySQL; HU pasa a `Completada`.

## Notas y decisiones
- PREGUNTA ABIERTA: qué representa “motivo cuando corresponda” en la decisión de reprogramación y cómo resolver colisión al aprobar.
