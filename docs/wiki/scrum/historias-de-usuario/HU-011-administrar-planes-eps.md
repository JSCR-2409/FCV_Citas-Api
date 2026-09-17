---
id: HU-011
tipo: historia-de-usuario
titulo: "Administrar planes de EPS"
estado: Pendiente de aprobación
epica: "[[EP-003-catalogos-y-gestion-de-profesionales]]"
esfuerzo: Medio
sprint_sugerido: "Incremento II — Acceso y datos maestros"
dependencias: ["[[HU-010-administrar-eps]]"]
relacionadas: ["[[HU-008-gestionar-afiliacion]]"]
---

# HU-011 — Administrar planes de EPS

## Historia de usuario
**COMO** ADMIN **QUIERO** gestionar planes vinculados a una EPS **PARA** ofrecer opciones de afiliación coherentes.

## Alcance
- Crear, consultar, actualizar y activar/desactivar planes asociados a EPS.

## Fuera de alcance
- Borrado físico de planes referenciados o asociarlos a múltiples EPS sin decisión aprobada.

## Reglas de negocio
- Plan pertenece a EPS; catálogo referenciado se desactiva en lugar de borrar; solo ADMIN.

## Dependencias y relaciones
- Épica: [[EP-003-catalogos-y-gestion-de-profesionales]]
- Dependencias: [[HU-010-administrar-eps]]
- Relacionada: [[HU-008-gestionar-afiliacion]]

## Esfuerzo
**Nivel:** Medio. Añade integridad jerárquica y efectos sobre afiliación.

## Tareas de desarrollo
- [ ] **T-01 — Precisar relación plan–EPS y activación.** Dificultad: Medio. Mantener 3FN.
- [ ] **T-02 — Implementar caso de uso, persistencia y contrato ADMIN.** Dificultad: Medio. Incluir migración si aplica.
- [ ] **T-03 — Integrar CRUD con filtrado por EPS y pruebas.** Dificultad: Medio. Validar rol y referencia.

## Criterios de aceptación
### CA-01 — Plan asociado
Dado un ADMIN y una EPS válida, cuando crea o edita un plan, entonces queda asociado a esa EPS.
### CA-02 — Integridad de referencia
Dado un plan usado por afiliación, cuando se solicita eliminar, entonces no se borra físicamente y se aplica desactivación si corresponde.
### CA-03 — Consulta coherente
Dado una EPS, cuando se consultan sus planes disponibles, entonces no aparecen planes de otra EPS.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de relación, referencia, RBAC y REST/UI aplicable.
- [ ] Hay migración Flyway cuando se modifica esquema y no se duplican datos de EPS.
- [ ] El contrato permite a [[HU-008-gestionar-afiliacion]] validar combinaciones sin inventar datos.
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
- PREGUNTA ABIERTA: atributos identificadores de un plan y alcance exacto de “cuando aplique”.
