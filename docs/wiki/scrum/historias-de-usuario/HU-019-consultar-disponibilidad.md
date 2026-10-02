---
id: HU-019
tipo: historia-de-usuario
titulo: "Consultar disponibilidad"
estado: Completada
epica: "[[EP-004-disponibilidad-y-busqueda-de-horarios]]"
esfuerzo: Alto
sprint_sugerido: "Incremento III — Oferta de agenda"
dependencias: ["[[HU-012-administrar-especialidades]]", "[[HU-014-asignar-especialidades-profesional]]", "[[HU-016-crear-bloques-de-disponibilidad]]"]
relacionadas: ["[[HU-020-reservar-cita-general]]", "[[HU-021-solicitar-cita-especializada]]"]
---

# HU-019 — Consultar disponibilidad

## Historia de usuario
**COMO** USER **QUIERO** filtrar horarios por sede, tipo de cita, especialidad, profesional y fecha **PARA** elegir una franja que complete la duración necesaria.

## Alcance
- Consulta de franjas elegibles por los cinco filtros del PRD; respuesta de 30/60 min según especialidad.

## Fuera de alcance
- Garantizar reserva hasta confirmación, mostrar franjas parciales o ofrecer profesional/especialidad/sede no elegibles.

## Reglas de negocio
- Solo horarios completos; 60 min requiere dos slots consecutivos; profesional habilitado en sede; especialidad activa y asociada; no slots reservados/retenidos.

## Dependencias y relaciones
- Épica: [[EP-004-disponibilidad-y-busqueda-de-horarios]]
- Dependencias: [[HU-012-administrar-especialidades]], [[HU-014-asignar-especialidades-profesional]], [[HU-016-crear-bloques-de-disponibilidad]]
- Relacionadas: [[HU-020-reservar-cita-general]], [[HU-021-solicitar-cita-especializada]]

## Esfuerzo
**Nivel:** Alto. Combina filtros, duración, asociaciones y estado dinámico de slots.

## Tareas de desarrollo
- [x] **T-01 — Definir consulta, filtros y proyección de disponibilidad.** Dificultad: Alto. Documentar orden/paginación si se aprueban.
- [x] **T-02 — Implementar cálculo de slots consecutivos y exclusiones.** Dificultad: Alto. Mantener consulta eficiente e íntegra.
- [x] **T-03 — Integrar búsqueda UI y estados de resultados.** Dificultad: Medio. No convertir la UI en autoridad de reglas.

## Criterios de aceptación
### CA-01 — Filtros aplicables
Dado una búsqueda con sede, tipo, especialidad, profesional y fecha, cuando existen coincidencias, entonces se muestran únicamente franjas que cumplen todos los filtros aplicables.
### CA-02 — Duración completa
Dado una especialidad de 60 minutos, cuando se consulta disponibilidad, entonces solo aparecen inicios con dos slots consecutivos libres.
### CA-03 — Exclusiones correctas
Dado slots retenidos/reservados, profesional no habilitado en sede o especialidad inactiva/no asociada, cuando se busca, entonces no se ofrecen como disponibles.

## Definition of Done
- [x] CA-01 a CA-03 tienen pruebas de dominio/aplicación/REST con combinaciones relevantes y UI aplicable.
- [x] La consulta utiliza datos 3FN e índices/migración Flyway cuando la implementación lo exige.
- [x] Contrato REST y frontend están alineados para filtros, resultados y errores sin BFF.
- [x] La trazabilidad Scrum está actualizada.

## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumplido | AvailabilityRulesTest.ca01_filtersByProfessionalAndLocation y ca01_returnedTimesMatchStoredSlots | Verificado el 2026-10-02 |
| CA-02 | Cumplido | AvailabilityRulesTest.ca02_sixtyMinutesRequiresTwoConsecutiveFreeSlots y ca02_thirtyMinutesOffersEveryFreeSlot; corregido el desplazamiento de zona que ofrecia una franja solapada | Verificado el 2026-10-02 |
| CA-03 | Cumplido | AvailabilityRulesTest.ca03_excludesInactiveSpecialty, ca03_excludesSpecialtyNotAssociatedToProfessional y ca03_excludesDeactivatedProfessional | Verificado el 2026-10-02 |
| DoD | Cumplida: pruebas REST, consulta sobre esquema 3FN, contrato alineado y UI de busqueda en el portal del paciente | Suite de 59 pruebas de backend en verde | Revisado el 2026-10-02 |

## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-02 — Implementacion verificada contra la suite de pruebas y el entorno MySQL; HU pasa a `Completada`.

## Notas y decisiones
- PREGUNTA ABIERTA: semántica de concurrencia entre lectura de disponibilidad y confirmación de reserva.
