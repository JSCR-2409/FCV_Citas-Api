---
id: HU-010
tipo: historia-de-usuario
titulo: "Administrar EPS"
estado: Pendiente de aprobación
epica: "[[EP-003-catalogos-y-gestion-de-profesionales]]"
esfuerzo: Medio
sprint_sugerido: "Incremento II — Acceso y datos maestros"
dependencias: ["[[HU-004-iniciar-sesion-jwt]]", "[[HU-009-consultar-catalogos-fijos]]"]
relacionadas: ["[[HU-011-administrar-planes-eps]]", "[[HU-008-gestionar-afiliacion]]"]
---

# HU-010 — Administrar EPS

## Historia de usuario
**COMO** ADMIN **QUIERO** crear, consultar, actualizar y desactivar EPS **PARA** mantener el catálogo configurable usado en afiliaciones.

## Alcance
- CRUD administrativo de EPS con desactivación cuando esté referenciada.

## Fuera de alcance
- Borrado físico de EPS referenciada, modificar afiliaciones de USER o EPS reales obligatorias.

## Reglas de negocio
- Un catálogo referenciado por transacciones no se borra físicamente; se activa/desactiva cuando aplique; solo ADMIN.

## Dependencias y relaciones
- Épica: [[EP-003-catalogos-y-gestion-de-profesionales]]
- Dependencias: [[HU-004-iniciar-sesion-jwt]], [[HU-009-consultar-catalogos-fijos]]
- Relacionadas: [[HU-011-administrar-planes-eps]], [[HU-008-gestionar-afiliacion]]

## Esfuerzo
**Nivel:** Medio. Combina RBAC, integridad referencial y administración UI/API.

## Tareas de desarrollo
- [ ] **T-01 — Modelar caso de uso y reglas de activación/referencia.** Dificultad: Medio. Definir unicidad aprobada si aplica.
- [ ] **T-02 — Implementar persistencia, migración y REST ADMIN.** Dificultad: Medio. Proteger contra eliminación indebida.
- [ ] **T-03 — Integrar CRUD UI y pruebas de rol/referencia.** Dificultad: Medio. Mostrar estado activo.

## Criterios de aceptación
### CA-01 — CRUD autorizado
Dado un ADMIN autenticado, cuando gestiona una EPS válida, entonces puede crear, consultar y actualizar según el contrato.
### CA-02 — Protección de referencias
Dado una EPS usada por una afiliación/transacción, cuando se pretende eliminar, entonces no se borra físicamente y se ofrece la desactivación aplicable.
### CA-03 — Restricción de rol
Dado USER o PROFESSIONAL, cuando intenta administrar EPS, entonces backend lo rechaza.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas RBAC, integración REST/persistencia y UI aplicable.
- [ ] El esquema y reglas de referencia están cubiertos por migración Flyway si cambian.
- [ ] Frontend consume el contrato aprobado directamente y no permite suplantar el control backend.
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
- PREGUNTA ABIERTA: campos y regla de unicidad de EPS no están especificados.
