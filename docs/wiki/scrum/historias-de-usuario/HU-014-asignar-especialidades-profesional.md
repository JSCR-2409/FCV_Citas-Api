---
id: HU-014
tipo: historia-de-usuario
titulo: "Asignar especialidades al profesional"
estado: Completada
epica: "[[EP-003-catalogos-y-gestion-de-profesionales]]"
esfuerzo: Medio
sprint_sugerido: "Incremento II — Acceso y datos maestros"
dependencias: ["[[HU-012-administrar-especialidades]]", "[[HU-013-crear-profesional]]"]
relacionadas: ["[[HU-019-consultar-disponibilidad]]"]
---

# HU-014 — Asignar especialidades al profesional

## Historia de usuario
**COMO** ADMIN **QUIERO** asociar una o varias especialidades activas y una primaria a un PROFESSIONAL **PARA** definir qué servicios puede ofrecer.

## Alcance
- Relación N:M profesional–especialidad y selección de especialidad primaria.

## Fuera de alcance
- Modificar duración de especialidad, asignar especialidad inactiva o más de una primaria.

## Reglas de negocio
- Una o varias especialidades; una primaria; especialidad activa y asociada para reservarse; duración pertenece a especialidad.

## Dependencias y relaciones
- Épica: [[EP-003-catalogos-y-gestion-de-profesionales]]
- Dependencias: [[HU-012-administrar-especialidades]], [[HU-013-crear-profesional]]
- Relacionada: [[HU-019-consultar-disponibilidad]]

## Esfuerzo
**Nivel:** Medio. Es una relación N:M con invariantes de estado y primaria.

## Tareas de desarrollo
- [x] **T-01 — Modelar relación y unicidad de primaria.** Dificultad: Medio. Evitar listas en columnas.
- [x] **T-02 — Implementar caso de uso ADMIN y validación de activo.** Dificultad: Medio. Añadir Flyway si corresponde.
- [x] **T-03 — Integrar gestión UI y pruebas de combinaciones.** Dificultad: Medio. No permitir duración editable.

## Criterios de aceptación
### CA-01 — Múltiples asociaciones
Dado un ADMIN y profesional existente, cuando asigna especialidades activas, entonces quedan asociadas sin duplicidad.
### CA-02 — Primaria única
Dado el profesional tiene asociaciones, cuando define la primaria, entonces existe exactamente una primaria entre sus especialidades.
### CA-03 — Reserva restringida
Dado una especialidad no activa o no asociada, cuando se intenta publicar/reservar para ella, entonces no se considera elegible.

## Definition of Done
- [x] CA-01 a CA-03 tienen pruebas de dominio/persistencia/RBAC y UI aplicable.
- [x] La tabla puente y regla de primaria mantienen 3FN y tienen Flyway si cambian esquema.
- [x] Disponibilidad/contrato consumen la asociación sin copiar datos de especialidad.
- [x] La trazabilidad Scrum está actualizada.

## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumplido | ProfessionalManagementTest.hu014_ca01_assignsSeveralSpecialtiesWithoutDuplicates | Verificado el 2026-10-02 |
| CA-02 | Cumplido | ProfessionalManagementTest.hu014_ca02_exactlyOnePrimarySpecialtyIsRequired | Verificado el 2026-10-02 |
| CA-03 | Cumplido | ProfessionalManagementTest.hu014_ca03_inactiveOrUnknownSpecialtyIsNotEligible y AvailabilityRulesTest.ca03_excludesSpecialtyNotAssociatedToProfessional | Verificado el 2026-10-02 |
| DoD | Cumplida: panel de asignacion con principal unica forzada en la UI | Suite de 88 pruebas de backend en verde | Revisado el 2026-10-02 |

## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-02 — Implementacion verificada contra la suite de pruebas y el entorno MySQL; HU pasa a `En validación`.
- 2026-10-02 — UI conectada al backend; DoD completada y HU pasa a `Completada`.

## Notas y decisiones
- PREGUNTA ABIERTA: comportamiento si se intenta desactivar una especialidad usada por citas futuras.
