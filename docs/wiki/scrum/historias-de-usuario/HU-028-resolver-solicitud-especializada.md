---
id: HU-028
tipo: historia-de-usuario
titulo: "Resolver solicitud especializada"
estado: Completada
epica: "[[EP-007-operacion-administrativa-y-auditoria]]"
esfuerzo: Alto
sprint_sugerido: "Incremento V — Operación clínica simulada"
dependencias: ["[[HU-027-consultar-bandeja-especializada]]"]
relacionadas: ["[[HU-031-auditar-cambios-de-estado]]"]
---

# HU-028 — Resolver solicitud especializada

## Historia de usuario
**COMO** ADMIN **QUIERO** aprobar o rechazar una solicitud especializada REQUESTED **PARA** decidir la cita y liberar la franja al rechazarla.

## Alcance
- Aprobar a APPROVED o rechazar a REJECTED con motivo obligatorio; actualizar slots y auditoría.

## Fuera de alcance
- Aprobar general, resolver solicitud no pendiente, rechazar sin motivo o editar la auditoría.

## Reglas de negocio
- Especializada requiere ADMIN; rechazo exige motivo y libera slots; transiciones explícitas, auditadas y sin doble reserva.

## Dependencias y relaciones
- Épica: [[EP-007-operacion-administrativa-y-auditoria]]
- Dependencias: [[HU-027-consultar-bandeja-especializada]]
- Relacionada: [[HU-031-auditar-cambios-de-estado]]

## Esfuerzo
**Nivel:** Alto. Coordina transiciones, retenciones, validación de motivo y concurrencia.

## Tareas de desarrollo
- [x] **T-01 — Definir matriz REQUESTED→APPROVED/REJECTED.** Dificultad: Alto. Documentar conflictos simultáneos.
- [x] **T-02 — Implementar decisión atómica, slots e historial.** Dificultad: Alto. Validar motivo en rechazo.
- [x] **T-03 — Integrar acciones ADMIN y pruebas de resultado USER.** Dificultad: Medio. Refrescar bandeja tras decisión.

## Criterios de aceptación
### CA-01 — Aprobación
Dado una solicitud REQUESTED, cuando ADMIN aprueba, entonces la cita queda APPROVED y conserva los slots retenidos.
### CA-02 — Rechazo con motivo
Dado una solicitud REQUESTED, cuando ADMIN rechaza con motivo, entonces queda REJECTED, el motivo se conserva y los slots se liberan.
### CA-03 — Decisión inválida
Dado un rechazo sin motivo o solicitud ya resuelta, cuando ADMIN intenta decidir, entonces se rechaza sin transición ni liberación incorrecta.

## Definition of Done
- [x] CA-01 a CA-03 tienen pruebas de transición, motivo, liberación, concurrencia y RBAC/UI.
- [x] Cita, retención y auditoría se actualizan coherentemente con Flyway si el esquema cambia.
- [x] USER puede observar estado/motivo conforme al contrato y evidencia cross-repo.
- [x] La trazabilidad Scrum está actualizada.

## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumplido | SpecializedRequestDecisionTest.hu028_ca01_approvalKeepsTheHeldSlots | Verificado el 2026-10-02 |
| CA-02 | Cumplido | SpecializedRequestDecisionTest.hu028_ca02_rejectionStoresTheReasonAndFreesTheSlots; el motivo se persiste en appointments.reason | Verificado el 2026-10-02 |
| CA-03 | Cumplido | SpecializedRequestDecisionTest.hu028_ca03_rejectionWithoutReasonIsRejected, hu028_ca03_anAlreadyResolvedRequestCannotBeDecidedAgain, hu028_ca03_aGeneralAppointmentCannotBeResolvedThroughThisEndpoint y hu028_ca03_unknownRequestReturnsNotFound | Verificado el 2026-10-02 |
| DoD | Cumplida: pruebas REST, guarda de estado corregida y UI de aprobacion/rechazo con motivo obligatorio | Suite de 59 pruebas de backend en verde | Revisado el 2026-10-02 |

## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-02 — Implementacion verificada contra la suite de pruebas y el entorno MySQL; HU pasa a `Completada`.

## Notas y decisiones
- PREGUNTA ABIERTA: notificación inmediata al USER es una automatización posterior, no requisito de esta decisión.
