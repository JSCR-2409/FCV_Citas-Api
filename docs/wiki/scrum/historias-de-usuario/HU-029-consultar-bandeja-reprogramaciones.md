---
id: HU-029
tipo: historia-de-usuario
titulo: "Consultar bandeja de reprogramaciones"
estado: Pendiente de aprobación
epica: "[[EP-007-operacion-administrativa-y-auditoria]]"
esfuerzo: Medio
sprint_sugerido: "Incremento V — Operación clínica simulada"
dependencias: ["[[HU-024-solicitar-reprogramacion]]"]
relacionadas: ["[[HU-030-resolver-reprogramacion]]"]
---

# HU-029 — Consultar bandeja de reprogramaciones

## Historia de usuario
**COMO** ADMIN **QUIERO** consultar reprogramaciones PENDING con filtros operativos **PARA** comparar la cita original y la propuesta antes de decidir.

## Alcance
- Bandeja PENDING filtrable por sede, profesional, especialidad y fecha, con datos de franja original/propuesta necesarios.

## Fuera de alcance
- Resolver desde lectura, mostrar solicitudes resueltas como PENDING o incluir cambios de profesional como reprogramación.

## Reglas de negocio
- ADMIN ve reprogramaciones PENDING; original se conserva; filtros del PRD.

## Dependencias y relaciones
- Épica: [[EP-007-operacion-administrativa-y-auditoria]]
- Dependencias: [[HU-024-solicitar-reprogramacion]]
- Relacionada: [[HU-030-resolver-reprogramacion]]

## Esfuerzo
**Nivel:** Medio. Es lectura comparativa autorizada de dos franjas relacionadas.

## Tareas de desarrollo
- [ ] **T-01 — Definir proyección original/propuesta y filtros.** Dificultad: Medio. No exponer atributos ajenos innecesarios.
- [ ] **T-02 — Implementar consulta ADMIN de estado PENDING.** Dificultad: Medio. Aplicar índices si se necesitan.
- [ ] **T-03 — Integrar bandeja y pruebas de filtros/privacidad.** Dificultad: Medio. Tratar estado vacío.

## Criterios de aceptación
### CA-01 — Solicitudes PENDING
Dado un ADMIN, cuando consulta bandeja, entonces ve solo reprogramaciones PENDING.
### CA-02 — Comparación suficiente
Dado una reprogramación pendiente, cuando se ve su detalle, entonces presenta profesional/especialidad conservados y franjas original/propuesta necesarias para decidir.
### CA-03 — Filtros y rol
Dado filtros de sede, profesional, especialidad o fecha, cuando se aplican, entonces delimitan resultados; otros roles no acceden.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de estado, proyección, filtros y RBAC/UI aplicable.
- [ ] El contrato no revela información fuera del propósito administrativo y documenta consulta vacía/error.
- [ ] Índices/migración Flyway existen si la implementación modifica esquema.
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
- PREGUNTA ABIERTA: orden/paginación y atributos exactos requeridos para comparación.
