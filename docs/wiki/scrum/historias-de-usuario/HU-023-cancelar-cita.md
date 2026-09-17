---
id: HU-023
tipo: historia-de-usuario
titulo: "Cancelar cita"
estado: Pendiente de aprobación
epica: "[[EP-005-ciclo-de-citas-del-usuario]]"
esfuerzo: Alto
sprint_sugerido: "Incremento IV — Reserva del usuario"
dependencias: ["[[HU-022-consultar-mis-citas]]"]
relacionadas: ["[[HU-031-auditar-cambios-de-estado]]"]
---

# HU-023 — Cancelar cita

## Historia de usuario
**COMO** USER autenticado **QUIERO** cancelar una cita futura no terminal propia **PARA** liberar su horario sin reactivar una cita cancelada.

## Alcance
- Transición a CANCELLED, liberación de slots e historial de cambio.

## Fuera de alcance
- Cancelar cita ajena/pasada/terminal, reactivar directamente CANCELLED o borrar la cita.

## Reglas de negocio
- Solo futura no terminal; CANCELLED libera slots; no reactivación directa; historial obligatorio.

## Dependencias y relaciones
- Épica: [[EP-005-ciclo-de-citas-del-usuario]]
- Dependencias: [[HU-022-consultar-mis-citas]]
- Relacionada: [[HU-031-auditar-cambios-de-estado]]

## Esfuerzo
**Nivel:** Alto. Cambia estado, reserva, visibilidad y auditoría de forma consistente.

## Tareas de desarrollo
- [ ] **T-01 — Definir estados terminales y transición autorizada.** Dificultad: Alto. No inferir estados no fijados.
- [ ] **T-02 — Implementar cancelación atómica con liberación/auditoría.** Dificultad: Alto. Proteger ownership.
- [ ] **T-03 — Integrar acción UI y pruebas de franja liberada.** Dificultad: Medio. Gestionar conflicto de estado.

## Criterios de aceptación
### CA-01 — Cancelación permitida
Dado una cita propia futura no terminal, cuando USER cancela, entonces queda CANCELLED y sus slots se liberan.
### CA-02 — Límites de cancelación
Dado una cita ajena, pasada o terminal, cuando USER intenta cancelar, entonces se rechaza y no cambia.
### CA-03 — Historial y no reactivación
Dado una cancelación exitosa, cuando se revisa historial/operación posterior, entonces se registra y no puede reactivarse directamente.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de transición, ownership, liberación y auditoría.
- [ ] La actualización de cita/slots/historial es consistente, con migración Flyway si se agrega esquema.
- [ ] Cliente refleja resultado/conflicto desde contrato REST sin asumir autorización local.
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
- PREGUNTA ABIERTA: conjunto exacto de estados terminales para efectos de cancelación.
