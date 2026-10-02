---
id: HU-012
tipo: historia-de-usuario
titulo: "Administrar especialidades"
estado: Completada
epica: "[[EP-003-catalogos-y-gestion-de-profesionales]]"
esfuerzo: Alto
sprint_sugerido: "Incremento II — Acceso y datos maestros"
dependencias: ["[[HU-004-iniciar-sesion-jwt]]"]
relacionadas: ["[[HU-014-asignar-especialidades-profesional]]", "[[HU-019-consultar-disponibilidad]]"]
---

# HU-012 — Administrar especialidades

## Historia de usuario
**COMO** ADMIN **QUIERO** gestionar especialidades activas y su duración de 30 o 60 minutos **PARA** configurar la oferta reservable.

## Alcance
- CRUD y activación/desactivación; cada especialidad define duración permitida 30/60 min.

## Fuera de alcance
- Duración arbitraria, borrar físicamente especialidad referenciada o permitir que PROFESSIONAL sobrescriba duración.

## Reglas de negocio
- Solo 30 o 60 min; especialidad activa y asociada al profesional para reservar; catálogos referenciados no se borran físicamente.

## Dependencias y relaciones
- Épica: [[EP-003-catalogos-y-gestion-de-profesionales]]
- Dependencias: [[HU-004-iniciar-sesion-jwt]]
- Relacionadas: [[HU-014-asignar-especialidades-profesional]], [[HU-019-consultar-disponibilidad]]

## Esfuerzo
**Nivel:** Alto. Su duración condiciona slots, reservas y disponibilidad posterior.

## Tareas de desarrollo
- [x] **T-01 — Modelar reglas de duración/estado y referencias.** Dificultad: Alto. Incluir Medicina General si la decisión de catálogo lo define.
- [x] **T-02 — Implementar administración y persistencia normalizada.** Dificultad: Medio. Aplicar Flyway si cambia esquema.
- [x] **T-03 — Integrar UI ADMIN y pruebas de duración/rol/referencia.** Dificultad: Medio. Exponer solo opciones autorizadas.

## Criterios de aceptación
### CA-01 — Duración restringida
Dado un ADMIN, cuando crea o actualiza una especialidad, entonces solo puede asignar 30 o 60 minutos.
### CA-02 — Estado y referencias
Dado una especialidad referenciada, cuando se intenta borrar, entonces se preserva y se desactiva cuando aplique.
### CA-03 — Autoridad de duración
Dado un PROFESSIONAL asociado, cuando publica/recibe disponibilidad, entonces no puede sustituir la duración de la especialidad.

## Definition of Done
- [x] CA-01 a CA-03 tienen pruebas de dominio, RBAC, persistencia y UI aplicable.
- [x] Cualquier esquema nuevo posee migración Flyway y relación 3FN.
- [x] El contrato es consumible por asignación/disponibilidad sin duplicar la especialidad.
- [x] La trazabilidad Scrum está actualizada.

## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumplido | CatalogAndAuthorizationTest.hu012_ca01_specialtyDurationIsRestrictedToThirtyOrSixty y hu012_ca01_updatingToAnInvalidDurationIsRejected | Verificado el 2026-10-02 |
| CA-02 | Cumplido | CatalogAndAuthorizationTest.hu012_ca02_adminSeesInactiveSpecialtiesToReactivateThem y hu012_ca02_onlyAdminCanListEverySpecialty | Verificado el 2026-10-02 |
| CA-03 | Cumplido | La duracion la resuelven /availability y la reserva desde specialties; el profesional no tiene endpoint para alterarla | Verificado el 2026-10-02 |
| DoD | Cumplida: alta, cambio de duracion y desactivacion desde el portal administrativo | Suite de 88 pruebas de backend en verde | Revisado el 2026-10-02 |

## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-02 — Implementacion verificada contra la suite de pruebas y el entorno MySQL; HU pasa a `En validación`.
- 2026-10-02 — UI conectada al backend; DoD completada y HU pasa a `Completada`.

## Notas y decisiones
- PREGUNTA ABIERTA: si “Medicina General” se precarga como especialidad y su política de activación.
