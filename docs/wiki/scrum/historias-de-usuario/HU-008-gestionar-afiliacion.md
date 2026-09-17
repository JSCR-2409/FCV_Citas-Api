---
id: HU-008
tipo: historia-de-usuario
titulo: "Gestionar afiliación"
estado: Pendiente de aprobación
epica: "[[EP-002-identidad-sesion-y-perfil]]"
esfuerzo: Medio
sprint_sugerido: "Incremento II — Acceso y datos maestros"
dependencias: ["[[HU-007-consultar-y-actualizar-perfil]]", "[[HU-010-administrar-eps]]", "[[HU-011-administrar-planes-eps]]"]
relacionadas: []
---

# HU-008 — Gestionar afiliación

## Historia de usuario
**COMO** USER autenticado **QUIERO** asociar mi EPS, plan y régimen mediante una afiliación **PARA** mantener mis datos administrativos sin duplicar catálogos.

## Alcance
- Crear o actualizar la afiliación propia con referencias a EPS, plan y régimen válidos.

## Fuera de alcance
- Duplicar nombres de EPS/plan/régimen en USER, gestionar afiliaciones ajenas o datos de aseguramiento reales.

## Reglas de negocio
- EPS, plan y régimen se relacionan como catálogo; plan corresponde a EPS; ownership del USER.

## Dependencias y relaciones
- Épica: [[EP-002-identidad-sesion-y-perfil]]
- Dependencias: [[HU-007-consultar-y-actualizar-perfil]], [[HU-010-administrar-eps]], [[HU-011-administrar-planes-eps]]

## Esfuerzo
**Nivel:** Medio. Une catálogos configurables, integridad relacional y perfil.

## Tareas de desarrollo
- [ ] **T-01 — Validar relación EPS–plan–régimen en caso de uso.** Dificultad: Medio. Usar referencias de catálogo.
- [ ] **T-02 — Persistir afiliación normalizada y autorizada.** Dificultad: Medio. Añadir migración si corresponde.
- [ ] **T-03 — Integrar formulario y pruebas de combinaciones inválidas.** Dificultad: Medio. No sustituir validación servidor.

## Criterios de aceptación
### CA-01 — Asociación válida
Dado un USER y catálogo activos válidos, cuando guarda afiliación, entonces esta se relaciona con EPS, plan y régimen sin copiar sus nombres.
### CA-02 — Coherencia de plan
Dado un plan que no corresponde a la EPS seleccionada, cuando intenta asociarlo, entonces se rechaza y no se persiste afiliación inconsistente.
### CA-03 — Ownership
Dado un USER, cuando consulta o cambia afiliación, entonces solo opera la propia.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de integridad, ownership y REST/UI si aplica.
- [ ] La estructura resultante cumple 3FN y tiene migración Flyway si se crea o modifica esquema.
- [ ] Catálogos/contrato implicados están alineados en ambos repositorios.
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
- El estado de activación de EPS/plan en una afiliación existente requiere contrato aprobado.
