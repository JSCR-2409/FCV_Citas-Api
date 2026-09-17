---
id: HU-026
tipo: historia-de-usuario
titulo: "Cerrar atención"
estado: Pendiente de aprobación
epica: "[[EP-006-operacion-del-profesional]]"
esfuerzo: Alto
sprint_sugerido: "Incremento V — Operación clínica simulada"
dependencias: ["[[HU-025-consultar-agenda-profesional]]"]
relacionadas: ["[[HU-031-auditar-cambios-de-estado]]"]
---

# HU-026 — Cerrar atención

## Historia de usuario
**COMO** PROFESSIONAL **QUIERO** marcar una cita propia pasada o aplicable como COMPLETED o NO_SHOW **PARA** reflejar el resultado operativo de la atención.

## Alcance
- Transiciones autorizadas desde cita aplicable a COMPLETED/NO_SHOW y registro de historial.

## Fuera de alcance
- Diagnóstico/tratamiento, cerrar citas ajenas/no aplicables o modificar auditoría como CRUD.

## Reglas de negocio
- Solo PROFESSIONAL de su propia cita; cita pasada/aplicable; transiciones explícitas y auditadas.

## Dependencias y relaciones
- Épica: [[EP-006-operacion-del-profesional]]
- Dependencias: [[HU-025-consultar-agenda-profesional]]
- Relacionada: [[HU-031-auditar-cambios-de-estado]]

## Esfuerzo
**Nivel:** Alto. Define transición sensible, elegibilidad temporal, ownership y auditoría.

## Tareas de desarrollo
- [ ] **T-01 — Aprobar definición de “pasada/aplicable” y transiciones origen.** Dificultad: Alto. No inferir una matriz ausente.
- [ ] **T-02 — Implementar caso de uso con ownership e historial.** Dificultad: Alto. Mantener consistencia de estado.
- [ ] **T-03 — Integrar acción UI y pruebas de permisos/transiciones.** Dificultad: Medio. Confirmar resultado observable.

## Criterios de aceptación
### CA-01 — Cierre autorizado
Dado una cita propia pasada/aplicable, cuando PROFESSIONAL la marca COMPLETED o NO_SHOW, entonces cambia al estado elegido y se registra historial.
### CA-02 — Límites de ownership/tiempo
Dado una cita ajena o no aplicable, cuando se intenta cerrar, entonces se rechaza sin modificar estado.
### CA-03 — Transición visible
Dado un cierre exitoso, cuando USER/ADMIN consultan la cita conforme a sus permisos, entonces ven el estado actualizado según contrato.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de matriz de transición, ownership, auditoría y REST/UI aplicable.
- [ ] La definición de aplicabilidad está aprobada o el ítem queda bloqueado de forma explícita.
- [ ] Cualquier cambio de esquema posee Flyway y el contrato comunica transición/errores.
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
- PREGUNTA ABIERTA: estados de origen y criterio exacto de “pasada/aplicable”.
