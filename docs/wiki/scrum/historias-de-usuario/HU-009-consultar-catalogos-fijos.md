---
id: HU-009
tipo: historia-de-usuario
titulo: "Consultar catálogos fijos"
estado: Pendiente de aprobación
epica: "[[EP-003-catalogos-y-gestion-de-profesionales]]"
esfuerzo: Medio
sprint_sugerido: "Incremento II — Acceso y datos maestros"
dependencias: ["[[HU-001-modelar-datos-3fn]]", "[[HU-002-documentar-contrato-rest-inicial]]"]
relacionadas: ["[[HU-010-administrar-eps]]"]
---

# HU-009 — Consultar catálogos fijos

## Historia de usuario
**COMO** usuario del sistema **QUIERO** consultar roles, estados de cita, estados de reprogramación, regímenes y sedes fijas **PARA** seleccionar valores consistentes en los flujos autorizados.

## Alcance
- Carga/seed y consulta de catálogos de solo lectura, incluidas HIC e ICV con datos autorizados.

## Fuera de alcance
- CRUD por ADMIN o cambiar sedes fijas.

## Reglas de negocio
- Roles, estados, regímenes y sedes son catálogos precargados, de solo lectura; no se usan datos privados.

## Dependencias y relaciones
- Épica: [[EP-003-catalogos-y-gestion-de-profesionales]]
- Dependencias: [[HU-001-modelar-datos-3fn]], [[HU-002-documentar-contrato-rest-inicial]]
- Relacionada: [[HU-010-administrar-eps]]

## Esfuerzo
**Nivel:** Medio. Requiere datos seed coherentes y contrato reusable por varias HU.

## Tareas de desarrollo
- [ ] **T-01 — Definir catálogo y seed autorizado.** Dificultad: Medio. Mantener IDs/nombres coherentes con PRD.
- [ ] **T-02 — Implementar lectura REST con autorización necesaria.** Dificultad: Medio. No exponer mutaciones.
- [ ] **T-03 — Integrar consumo UI y pruebas de inmutabilidad.** Dificultad: Bajo. Ajustar al framework decidido.

## Criterios de aceptación
### CA-01 — Catálogos disponibles
Dado el sistema inicializado, cuando se consultan catálogos fijos, entonces devuelve roles, estados, regímenes y las dos sedes definidas.
### CA-02 — Solo lectura
Dado un actor, cuando intenta crear, editar o borrar un valor fijo por la interfaz/contrato de catálogo, entonces no existe operación autorizada.
### CA-03 — Datos de laboratorio
Dado el contenido de catálogos, cuando se revisa, entonces no incorpora datos sensibles de pacientes o profesionales reales.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de seed/REST/UI aplicable.
- [ ] Si el esquema/seed se introduce, hay migración Flyway reproducible.
- [ ] El contrato describe valores y acceso de solo lectura sin contradicción con PRD.
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
- La forma de localizar los catálogos por API se define en [[HU-002-documentar-contrato-rest-inicial]].
