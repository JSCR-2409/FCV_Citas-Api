---
id: HU-018
tipo: historia-de-usuario
titulo: "Consultar calendario profesional"
estado: Completada
epica: "[[EP-004-disponibilidad-y-busqueda-de-horarios]]"
esfuerzo: Medio
sprint_sugerido: "Incremento III — Oferta de agenda"
dependencias: ["[[HU-016-crear-bloques-de-disponibilidad]]"]
relacionadas: ["[[HU-017-modificar-bloques-futuros]]"]
---

# HU-018 — Consultar calendario profesional

## Historia de usuario
**COMO** PROFESSIONAL **QUIERO** consultar mi calendario de bloques **PARA** conocer y administrar mi disponibilidad publicada.

## Alcance
- Vista de bloques propios por período/sede según contrato.

## Fuera de alcance
- Ver bloques de otros profesionales o detalles de citas no autorizados.

## Reglas de negocio
- Ownership del profesional; solo datos de su calendario; filtros concretos se documentan en contrato.

## Dependencias y relaciones
- Épica: [[EP-004-disponibilidad-y-busqueda-de-horarios]]
- Dependencias: [[HU-016-crear-bloques-de-disponibilidad]]
- Relacionada: [[HU-017-modificar-bloques-futuros]]

## Esfuerzo
**Nivel:** Medio. Requiere proyección autorizada, filtros y estado de UI.

## Tareas de desarrollo
- [x] **T-01 — Definir lectura y filtros de calendario.** Dificultad: Medio. No asumir paginación/periodo sin contrato.
- [x] **T-02 — Implementar consulta con ownership.** Dificultad: Medio. Excluir información no autorizada.
- [x] **T-03 — Integrar calendario y estados loading/empty/error.** Dificultad: Medio. Validar navegación/diseño aprobado.

## Criterios de aceptación
### CA-01 — Vista propia
Dado un PROFESSIONAL autenticado con bloques, cuando consulta calendario, entonces observa sus bloques publicados con fecha, hora y sede.
### CA-02 — Aislamiento
Dado otro profesional, cuando intenta consultar un calendario ajeno por identificación manipulada, entonces backend lo rechaza o no entrega datos ajenos.
### CA-03 — Sin bloques
Dado un PROFESSIONAL sin bloques en el criterio consultado, cuando abre calendario, entonces recibe un estado vacío claro sin inventar disponibilidad.

## Definition of Done
- [x] CA-01 a CA-03 tienen pruebas de ownership, REST y UI aplicable.
- [x] El contrato delimita filtros/atributos y la vista no muestra tokens ni datos de USER.
- [x] Accesibilidad/estados de carga, vacío y error se verifican frente al diseño aprobado cuando exista.
- [x] La trazabilidad Scrum está actualizada.

## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumplido | AvailabilityBlockTest.hu018_ca01_calendarShowsOwnBlocksWithDateTimeAndSite | Verificado el 2026-10-02 |
| CA-02 | Cumplido | AvailabilityBlockTest.hu018_ca02_calendarNeverExposesAnotherProfessionalsBlocks | Verificado el 2026-10-02 |
| CA-03 | Cumplido | AvailabilityBlockTest.hu018_ca03_emptyRangeReturnsAnEmptyStateWithoutInventingAvailability | Verificado el 2026-10-02 |
| DoD | Cumplida: calendario propio con rango configurable en el portal del profesional | Suite de 88 pruebas de backend en verde | Revisado el 2026-10-02 |

## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-02 — Implementacion verificada contra la suite de pruebas y el entorno MySQL; HU pasa a `En validación`.
- 2026-10-02 — UI conectada al backend; DoD completada y HU pasa a `Completada`.

## Notas y decisiones
- PREGUNTA ABIERTA: período por defecto, paginación y representación de bloques reservados.
