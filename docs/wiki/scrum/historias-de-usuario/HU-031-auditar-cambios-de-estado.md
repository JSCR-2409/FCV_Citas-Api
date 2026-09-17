---
id: HU-031
tipo: historia-de-usuario
titulo: "Auditar cambios de estado"
estado: Pendiente de aprobación
epica: "[[EP-007-operacion-administrativa-y-auditoria]]"
esfuerzo: Alto
sprint_sugerido: "Incremento V — Operación clínica simulada"
dependencias: ["[[HU-020-reservar-cita-general]]", "[[HU-028-resolver-solicitud-especializada]]", "[[HU-030-resolver-reprogramacion]]", "[[HU-026-cerrar-atencion]]"]
relacionadas: ["[[HU-033-notificar-cambios-de-estado]]"]
---

# HU-031 — Auditar cambios de estado

## Historia de usuario
**COMO** ADMIN autorizado **QUIERO** contar con un historial inmutable de cambios de estado de cita **PARA** rastrear qué ocurrió, quién lo originó y por qué.

## Alcance
- Registrar y consultar, según permisos aprobados, cita, estado nuevo, actor cuando exista, fuente SYSTEM/USER/ADMIN, fecha/hora y motivo opcional.

## Fuera de alcance
- CRUD normal de auditoría, alterar eventos pasados o almacenar datos clínicos.

## Reglas de negocio
- Todo cambio de estado se registra; fuente permitida SYSTEM/USER/ADMIN; auditoría no se modifica como CRUD normal.

## Dependencias y relaciones
- Épica: [[EP-007-operacion-administrativa-y-auditoria]]
- Dependencias: [[HU-020-reservar-cita-general]], [[HU-028-resolver-solicitud-especializada]], [[HU-030-resolver-reprogramacion]], [[HU-026-cerrar-atencion]]
- Relacionada: [[HU-033-notificar-cambios-de-estado]]

## Esfuerzo
**Nivel:** Alto. Es transversal a estados, autorización y trazabilidad temporal.

## Tareas de desarrollo
- [ ] **T-01 — Definir evento de historial y fuente/actor.** Dificultad: Alto. Alinear todas las transiciones aprobadas.
- [ ] **T-02 — Implementar registro transaccional inmutable.** Dificultad: Alto. No habilitar mutación CRUD.
- [ ] **T-03 — Implementar lectura autorizada y pruebas de cobertura.** Dificultad: Medio. Preparar evento para automatización posterior.

## Criterios de aceptación
### CA-01 — Evento completo
Dado un cambio de estado de cita, cuando finaliza, entonces se registra cita, nuevo estado, actor cuando existe, fuente, fecha/hora y motivo opcional.
### CA-02 — Cobertura de fuentes
Dado un cambio originado por SYSTEM, USER o ADMIN, cuando se revisa historial, entonces contiene la fuente correcta.
### CA-03 — Inmutabilidad operativa
Dado un evento existente, cuando se intenta editarlo o borrarlo mediante CRUD normal, entonces no existe operación autorizada.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de todas las transiciones implementadas, fuente/actor y control de mutación.
- [ ] Historial tiene migración Flyway, claves/índices y consistencia transaccional cuando se implementa.
- [ ] La lectura autorizada no expone datos indebidos y el contrato queda documentado.
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
- PREGUNTA ABIERTA: qué roles pueden consultar el historial y el nivel de detalle visible a USER/PROFESSIONAL.
