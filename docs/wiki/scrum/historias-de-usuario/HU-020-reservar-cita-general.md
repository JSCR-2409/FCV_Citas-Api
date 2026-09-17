---
id: HU-020
tipo: historia-de-usuario
titulo: "Reservar cita general"
estado: Pendiente de aprobación
epica: "[[EP-005-ciclo-de-citas-del-usuario]]"
esfuerzo: Alto
sprint_sugerido: "Incremento IV — Reserva del usuario"
dependencias: ["[[HU-019-consultar-disponibilidad]]"]
relacionadas: ["[[HU-022-consultar-mis-citas]]", "[[HU-031-auditar-cambios-de-estado]]"]
---

# HU-020 — Reservar cita general

## Historia de usuario
**COMO** USER autenticado **QUIERO** seleccionar Medicina General, profesional y horario disponible **PARA** obtener una cita aprobada automáticamente.

## Alcance
- Confirmación de una franja disponible de Medicina General y creación en estado APPROVED.

## Fuera de alcance
- Aprobación ADMIN, reservar franjas ya ocupadas/retenidas o usar especialidad distinta de Medicina General.

## Reglas de negocio
- USER selecciona Medicina General y profesional general disponible; si persiste disponibilidad al confirmar, se crea APPROVED; no doble reserva.

## Dependencias y relaciones
- Épica: [[EP-005-ciclo-de-citas-del-usuario]]
- Dependencias: [[HU-019-consultar-disponibilidad]]
- Relacionadas: [[HU-022-consultar-mis-citas]], [[HU-031-auditar-cambios-de-estado]]

## Esfuerzo
**Nivel:** Alto. Requiere confirmación transaccional de slots, estado y auditoría.

## Tareas de desarrollo
- [ ] **T-01 — Precisar selección de Medicina General/profesional.** Dificultad: Medio. Resolver catálogo en contrato aprobado.
- [ ] **T-02 — Implementar caso de reserva atómica y transición APPROVED.** Dificultad: Alto. Evitar doble reserva concurrente.
- [ ] **T-03 — Integrar confirmación UI y pruebas end-to-end de contrato.** Dificultad: Alto. Manejar pérdida de disponibilidad.

## Criterios de aceptación
### CA-01 — Aprobación automática
Dado un USER y una franja general aún disponible, cuando confirma, entonces se crea una cita en estado APPROVED sin intervención ADMIN.
### CA-02 — Protección contra carrera
Dado que la franja deja de estar libre antes de confirmar, cuando el USER intenta reservar, entonces la creación se rechaza y no comparte slots con otra cita/retención.
### CA-03 — Trazabilidad de resultado
Dado una reserva creada, cuando USER consulta sus citas e historial aplicable, entonces aparece con profesional, sede, fecha/hora, duración y estado APPROVED.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de concurrencia/reglas, REST/persistencia y flujo frontend aplicable.
- [ ] Cita, slots y auditoría usan migración Flyway/índices cuando son nuevos; no se rompe 3FN.
- [ ] El contrato registra respuesta de conflicto y evidencia cross-repo para la confirmación.
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
- PREGUNTA ABIERTA: cómo se identifica/pre-carga Medicina General y el código de conflicto de reserva.
