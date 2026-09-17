---
id: HU-007
tipo: historia-de-usuario
titulo: "Consultar y actualizar perfil"
estado: Pendiente de aprobación
epica: "[[EP-002-identidad-sesion-y-perfil]]"
esfuerzo: Medio
sprint_sugerido: "Incremento II — Acceso y datos maestros"
dependencias: ["[[HU-004-iniciar-sesion-jwt]]"]
relacionadas: ["[[HU-008-gestionar-afiliacion]]"]
---

# HU-007 — Consultar y actualizar perfil

## Historia de usuario
**COMO** USER autenticado **QUIERO** consultar y modificar mis datos permitidos **PARA** mantener mi información de contacto vigente.

## Alcance
- Consulta y actualización de atributos de perfil que se aprueben por contrato, con ownership del USER.

## Fuera de alcance
- Editar roles, perfiles de terceros, datos clínicos o atributos no permitidos.

## Reglas de negocio
- Autorización por ownership; validación server-side; campos permitidos pendientes de concretar en contrato.

## Dependencias y relaciones
- Épica: [[EP-002-identidad-sesion-y-perfil]]
- Dependencias: [[HU-004-iniciar-sesion-jwt]]
- Relacionada: [[HU-008-gestionar-afiliacion]]

## Esfuerzo
**Nivel:** Medio. Requiere límites claros de edición y consistencia con identidad.

## Tareas de desarrollo
- [ ] **T-01 — Aprobar matriz de campos editables.** Dificultad: Medio. Registrar los no definidos como decisión.
- [ ] **T-02 — Implementar consulta/actualización con ownership y validación.** Dificultad: Medio. Separar caso de uso y adaptadores.
- [ ] **T-03 — Integrar pantalla de perfil y pruebas de acceso.** Dificultad: Medio. Mostrar estados de éxito/error.

## Criterios de aceptación
### CA-01 — Consulta propia
Dado un USER autenticado, cuando consulta su perfil, entonces ve únicamente su información autorizada.
### CA-02 — Actualización válida
Dado un cambio válido en campo permitido, cuando lo guarda, entonces se persiste y se refleja en una consulta posterior.
### CA-03 — Protección de límites
Dado un campo no permitido o un perfil ajeno, cuando se intenta modificar, entonces backend lo rechaza sin alterar datos.

## Definition of Done
- [ ] CA-01 a CA-03 están probados en aplicación/REST y UI aplicable.
- [ ] La matriz de campos editables está aprobada o la HU conserva el impedimento documentado.
- [ ] Si cambia esquema, existe migración Flyway y contrato actualizado en ambos repositorios.
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
- PREGUNTA ABIERTA: lista exacta de datos permitidos para actualización.
