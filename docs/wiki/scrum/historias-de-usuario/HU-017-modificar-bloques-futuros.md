---
id: HU-017
tipo: historia-de-usuario
titulo: "Modificar bloques futuros"
estado: Pendiente de aprobación
epica: "[[EP-004-disponibilidad-y-busqueda-de-horarios]]"
esfuerzo: Alto
sprint_sugerido: "Incremento III — Oferta de agenda"
dependencias: ["[[HU-016-crear-bloques-de-disponibilidad]]"]
relacionadas: ["[[HU-018-consultar-calendario-profesional]]"]
---

# HU-017 — Modificar bloques futuros

## Historia de usuario
**COMO** PROFESSIONAL **QUIERO** editar o eliminar mis bloques futuros sin citas comprometidas **PARA** mantener mi agenda disponible actualizada.

## Alcance
- Editar/eliminar bloques futuros propios que no tengan citas/reservas comprometidas.

## Fuera de alcance
- Alterar bloques pasados, ajenos o con citas comprometidas.

## Reglas de negocio
- Solo futuro y ownership; no modificar/eliminar si hay cita comprometida; tras edición se mantienen reglas de sede y no solape.

## Dependencias y relaciones
- Épica: [[EP-004-disponibilidad-y-busqueda-de-horarios]]
- Dependencias: [[HU-016-crear-bloques-de-disponibilidad]]
- Relacionada: [[HU-018-consultar-calendario-profesional]]

## Esfuerzo
**Nivel:** Alto. Debe detectar compromisos y preservar la integridad de reservas.

## Tareas de desarrollo
- [ ] **T-01 — Definir qué constituye cita comprometida.** Dificultad: Alto. Alinear estados/retenciones con contrato.
- [ ] **T-02 — Implementar edición/eliminación con ownership e invariantes.** Dificultad: Alto. Revalidar solape/sede/futuro.
- [ ] **T-03 — Integrar acciones de calendario y pruebas de bloqueo.** Dificultad: Medio. Confirmar resultados observables.

## Criterios de aceptación
### CA-01 — Modificación permitida
Dado un bloque propio futuro sin cita comprometida, cuando se edita o elimina con valores válidos, entonces la agenda se actualiza.
### CA-02 — Protección de bloques comprometidos
Dado un bloque con cita comprometida, cuando se intenta editar o eliminar, entonces se rechaza y la cita/agenda se conserva.
### CA-03 — Protección temporal y ownership
Dado un bloque pasado o de otro profesional, cuando se intenta mutar, entonces se rechaza sin efecto.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de estados de reserva, ownership y REST/UI aplicable.
- [ ] Si existe actualización de slots, esta es transaccional y tiene migración Flyway si cambia esquema.
- [ ] La definición de “comprometida” está aprobada o registrada como bloqueo no implementable.
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
- PREGUNTA ABIERTA: estados/retenciones que hacen un bloque “comprometido”.
