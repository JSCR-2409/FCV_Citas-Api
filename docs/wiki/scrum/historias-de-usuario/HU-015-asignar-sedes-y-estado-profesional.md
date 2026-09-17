---
id: HU-015
tipo: historia-de-usuario
titulo: "Asignar sedes y estado al profesional"
estado: Pendiente de aprobación
epica: "[[EP-003-catalogos-y-gestion-de-profesionales]]"
esfuerzo: Medio
sprint_sugerido: "Incremento II — Acceso y datos maestros"
dependencias: ["[[HU-013-crear-profesional]]", "[[HU-009-consultar-catalogos-fijos]]"]
relacionadas: ["[[HU-016-crear-bloques-de-disponibilidad]]"]
---

# HU-015 — Asignar sedes y estado al profesional

## Historia de usuario
**COMO** ADMIN **QUIERO** asignar una o ambas sedes fijas y activar/desactivar un PROFESSIONAL **PARA** controlar dónde puede publicar agenda y ofrecer citas.

## Alcance
- Asociación N:M con HIC/ICV y cambio de estado operativo del profesional.

## Fuera de alcance
- Crear sedes, permitir agenda en sede no asignada o borrar físicamente al profesional.

## Reglas de negocio
- Profesional trabaja en una o ambas sedes; solo publica bloques en sede asignada; activo/inactivo condiciona operación conforme a contrato.

## Dependencias y relaciones
- Épica: [[EP-003-catalogos-y-gestion-de-profesionales]]
- Dependencias: [[HU-013-crear-profesional]], [[HU-009-consultar-catalogos-fijos]]
- Relacionada: [[HU-016-crear-bloques-de-disponibilidad]]

## Esfuerzo
**Nivel:** Medio. Afecta habilitación, disponibilidad y citas futuras.

## Tareas de desarrollo
- [ ] **T-01 — Modelar relación profesional–sede y estado.** Dificultad: Medio. Usar catálogo fijo.
- [ ] **T-02 — Implementar administración RBAC y reglas de habilitación.** Dificultad: Medio. Incluir migración si aplica.
- [ ] **T-03 — Integrar UI y pruebas de sede no asignada/inactivo.** Dificultad: Medio. Alinear efectos con contrato.

## Criterios de aceptación
### CA-01 — Sedes permitidas
Dado un ADMIN, cuando asigna una o ambas sedes fijas a un profesional, entonces quedan disponibles para su configuración de agenda.
### CA-02 — Sede no asignada
Dado una sede no asociada, cuando el profesional intenta crear bloque allí, entonces se rechaza.
### CA-03 — Estado operativo
Dado un profesional desactivado, cuando intenta operar una capacidad restringida o se ofrece para disponibilidad, entonces se aplica la restricción aprobada por contrato.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de asociación, RBAC y uso por disponibilidad.
- [ ] El estado/relación conserva 3FN y Flyway cuando se modifica esquema.
- [ ] Los efectos de desactivar con citas/bloques existentes están documentados o bloqueados como decisión pendiente.
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
- PREGUNTA ABIERTA: efecto de la desactivación sobre bloques y citas existentes/futuras.
