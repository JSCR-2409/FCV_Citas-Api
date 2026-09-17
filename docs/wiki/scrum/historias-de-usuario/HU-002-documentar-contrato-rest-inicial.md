---
id: HU-002
tipo: historia-de-usuario
titulo: "Documentar contrato REST inicial"
estado: Pendiente de aprobación
epica: "[[EP-001-fundacion-de-datos-y-contrato-rest]]"
esfuerzo: Alto
sprint_sugerido: "Incremento I — Base especificable"
dependencias: ["[[HU-001-modelar-datos-3fn]]"]
relacionadas: ["[[HU-003-registrar-usuario]]", "[[HU-019-consultar-disponibilidad]]"]
---

# HU-002 — Documentar contrato REST inicial

## Historia de usuario
**COMO** equipo de producto **QUIERO** acordar un contrato REST JSON trazable **PARA** integrar frontend y backend directamente sin inventar interfaces.

## Alcance
- Documentar recursos, entradas, salidas, errores, autenticación, autorización y compatibilidad para las HU que se aprueben.
- Registrar decisiones pendientes de concurrencia, filtros y estados.

## Fuera de alcance
- Implementar controladores, DTO, clientes frontend, BFF o elegir framework UI.

## Reglas de negocio
- REST directo a `citas-api`; roles en autorización; access y refresh separados; CORS explícito; no exponer tokens/passwords.

## Dependencias y relaciones
- Épica: [[EP-001-fundacion-de-datos-y-contrato-rest]]
- Dependencias: [[HU-001-modelar-datos-3fn]]
- Relacionadas: [[HU-003-registrar-usuario]], [[HU-019-consultar-disponibilidad]]

## Esfuerzo
**Nivel:** Alto. Es transversal, bloquea integración y requiere aprobaciones explícitas.

## Tareas de desarrollo
- [ ] **T-01 — Mapear capacidades aprobadas a recursos REST.** Dificultad: Medio. Mantener trazabilidad a HU/RF.
- [ ] **T-02 — Precisar modelos de mensaje, errores y permisos.** Dificultad: Alto. No inventar detalles sin marcar decisión.
- [ ] **T-03 — Registrar estrategia de evolución y evidencia cross-repo.** Dificultad: Medio. Definir cómo se verificará la compatibilidad.

## Criterios de aceptación
### CA-01 — Contrato trazable
Dado una HU dependiente, cuando se consulte el contrato, entonces identifica interacción REST, autorización y mensaje esperado o una pregunta abierta explícita.
### CA-02 — Integración directa y segura
Dado el contrato, cuando se revise, entonces no incluye Express/BFF ni datos sensibles en mensajes, logs o documentación.
### CA-03 — Ambigüedades visibles
Dado filtros, concurrencia, retenciones o errores no especificados, cuando no exista fuente autorizada, entonces aparecen como PREGUNTA ABIERTA y no como decisión implícita.

## Definition of Done
- [ ] CA-01 a CA-03 tienen evidencia de revisión por frontend y backend cuando corresponda.
- [ ] El contrato versionable relaciona recursos con HU y no contradice PRD/restricciones.
- [ ] Existe ruta de verificación cross-repo para cada cambio de contrato aprobado.
- [ ] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

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
- PREGUNTA ABIERTA: nomenclatura concreta de recursos, errores, paginación y exclusión ante reserva simultánea.
