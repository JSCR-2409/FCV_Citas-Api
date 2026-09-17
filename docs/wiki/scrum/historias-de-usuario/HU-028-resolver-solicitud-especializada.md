---
id: HU-028
tipo: historia-de-usuario
titulo: "Resolver solicitud especializada"
estado: Pendiente de aprobación
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
- [ ] **T-01 — Definir matriz REQUESTED→APPROVED/REJECTED.** Dificultad: Alto. Documentar conflictos simultáneos.
- [ ] **T-02 — Implementar decisión atómica, slots e historial.** Dificultad: Alto. Validar motivo en rechazo.
- [ ] **T-03 — Integrar acciones ADMIN y pruebas de resultado USER.** Dificultad: Medio. Refrescar bandeja tras decisión.

## Criterios de aceptación
### CA-01 — Aprobación
Dado una solicitud REQUESTED, cuando ADMIN aprueba, entonces la cita queda APPROVED y conserva los slots retenidos.
### CA-02 — Rechazo con motivo
Dado una solicitud REQUESTED, cuando ADMIN rechaza con motivo, entonces queda REJECTED, el motivo se conserva y los slots se liberan.
### CA-03 — Decisión inválida
Dado un rechazo sin motivo o solicitud ya resuelta, cuando ADMIN intenta decidir, entonces se rechaza sin transición ni liberación incorrecta.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de transición, motivo, liberación, concurrencia y RBAC/UI.
- [ ] Cita, retención y auditoría se actualizan coherentemente con Flyway si el esquema cambia.
- [ ] USER puede observar estado/motivo conforme al contrato y evidencia cross-repo.
- [ ] La trazabilidad Scrum está actualizada.

## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Pendiente | — | — |
| CA-02 | Pendiente | — | — |
| CA-03 | Pendiente | — | — |
| DoD | Pendiente | — | — |

## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.

## Notas y decisiones
- PREGUNTA ABIERTA: notificación inmediata al USER es una automatización posterior, no requisito de esta decisión.
