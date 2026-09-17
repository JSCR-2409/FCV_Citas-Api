---
id: HU-016
tipo: historia-de-usuario
titulo: "Crear bloques de disponibilidad"
estado: Pendiente de aprobación
epica: "[[EP-004-disponibilidad-y-busqueda-de-horarios]]"
esfuerzo: Alto
sprint_sugerido: "Incremento III — Oferta de agenda"
dependencias: ["[[HU-014-asignar-especialidades-profesional]]", "[[HU-015-asignar-sedes-y-estado-profesional]]"]
relacionadas: ["[[HU-017-modificar-bloques-futuros]]", "[[HU-019-consultar-disponibilidad]]"]
---

# HU-016 — Crear bloques de disponibilidad

## Historia de usuario
**COMO** PROFESSIONAL **QUIERO** crear uno o varios bloques futuros por día y sede asignada **PARA** publicar mi disponibilidad reservable.

## Alcance
- Crear bloques de inicio/fin, fecha y sede; discretizarlos en slots de 30 min.

## Fuera de alcance
- Fechas pasadas, solapamientos, sedes no asignadas y duración configurada por el profesional.

## Reglas de negocio
- No pasado ni solapado para mismo profesional; sede asignada; múltiples bloques diarios; slots de 30 min.

## Dependencias y relaciones
- Épica: [[EP-004-disponibilidad-y-busqueda-de-horarios]]
- Dependencias: [[HU-014-asignar-especialidades-profesional]], [[HU-015-asignar-sedes-y-estado-profesional]]
- Relacionadas: [[HU-017-modificar-bloques-futuros]], [[HU-019-consultar-disponibilidad]]

## Esfuerzo
**Nivel:** Alto. Involucra validaciones temporales, sede, solape y generación de slots.

## Tareas de desarrollo
- [ ] **T-01 — Definir modelo/caso de uso de bloque y slots.** Dificultad: Alto. Acordar límites de hora/fecha en contrato.
- [ ] **T-02 — Implementar validaciones de pasado, sede y solape.** Dificultad: Alto. Proteger concurrencia de escritura.
- [ ] **T-03 — Integrar calendario/formulario y pruebas.** Dificultad: Medio. Mostrar errores verificables.

## Criterios de aceptación
### CA-01 — Bloques múltiples válidos
Dado un PROFESSIONAL activo y sede asignada, cuando crea bloques futuros no solapados, entonces se publican y se discretizan en slots de 30 min.
### CA-02 — Prevención de inválidos
Dado una fecha pasada, sede no asignada o intervalo solapado, cuando intenta crear bloque, entonces se rechaza sin modificar agenda.
### CA-03 — Ejemplo diario
Dado el mismo día, cuando crea 08:00–12:00 y 14:00–17:00 en HIC, entonces ambos bloques son válidos y el intervalo intermedio no se ofrece.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de dominio/aplicación/REST y UI calendario aplicable.
- [ ] Datos de bloques/slots tienen migración Flyway e índices pertinentes si se implementan.
- [ ] Autorización/ownership del profesional y contrato REST directo están verificados.
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
- PREGUNTA ABIERTA: zona horaria, granularidad de límites y estrategia de concurrencia de bloques.
