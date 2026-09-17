---
id: HU-024
tipo: historia-de-usuario
titulo: "Solicitar reprogramación"
estado: Pendiente de aprobación
epica: "[[EP-005-ciclo-de-citas-del-usuario]]"
esfuerzo: Muy alto
sprint_sugerido: "Incremento IV — Reserva del usuario"
dependencias: ["[[HU-022-consultar-mis-citas]]", "[[HU-019-consultar-disponibilidad]]"]
relacionadas: ["[[HU-029-consultar-bandeja-reprogramaciones]]", "[[HU-030-resolver-reprogramacion]]"]
---

# HU-024 — Solicitar reprogramación

## Historia de usuario
**COMO** USER autenticado **QUIERO** solicitar una nueva franja disponible para una cita aprobada futura sin cambiar profesional/especialidad **PARA** conservar mi cita original hasta que ADMIN decida.

## Alcance
- Crear solicitud PENDING, retener nueva franja y conservar original; mostrar alternativa de conservar o cancelar tras rechazo.

## Fuera de alcance
- Cambiar profesional como reprogramación, destruir la cita original al solicitar o aprobar sin ADMIN.

## Reglas de negocio
- Solo APPROVED futura; mantiene profesional/especialidad; nueva franja se retiene en PENDING; original permanece hasta decisión; cambio de profesional es cita nueva.

## Dependencias y relaciones
- Épica: [[EP-005-ciclo-de-citas-del-usuario]]
- Dependencias: [[HU-022-consultar-mis-citas]], [[HU-019-consultar-disponibilidad]]
- Relacionadas: [[HU-029-consultar-bandeja-reprogramaciones]], [[HU-030-resolver-reprogramacion]]

## Esfuerzo
**Nivel:** Muy alto. Reúne dos franjas, estados coordinados, retención y reglas de preservación; se divide con la decisión ADMIN en HU-030.

## Tareas de desarrollo
- [ ] **T-01 — Modelar solicitud PENDING y vínculos original/propuesta.** Dificultad: Alto. Mantener 3FN y retención clara.
- [ ] **T-02 — Implementar validación y retención atómica de nueva franja.** Dificultad: Alto. Proteger contra doble reserva.
- [ ] **T-03 — Integrar selección UI, detalle y pruebas de preservación.** Dificultad: Alto. Explicar estado PENDING al USER.

## Criterios de aceptación
### CA-01 — Elegibilidad
Dado una cita propia APPROVED y futura, cuando USER selecciona nueva franja para mismo profesional/especialidad, entonces puede crear una solicitud PENDING.
### CA-02 — Doble conservación
Dado una solicitud PENDING creada, cuando se consulta agenda, entonces la nueva franja está retenida y la cita original conserva su franja.
### CA-03 — Cambios no permitidos
Dado un intento de reprogramar cita no aprobada/no futura o cambiar profesional, cuando se confirma, entonces se rechaza o se indica que es nueva cita según contrato.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de reglas, concurrencia y persistencia/REST/UI.
- [ ] Solicitud, retenciones y relación con cita original tienen Flyway/índices y no rompen 3FN.
- [ ] La decisión ADMIN permanece separada en [[HU-030-resolver-reprogramacion]] y el contrato comunica PENDING/conflictos.
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
- PREGUNTA ABIERTA: caducidad de retención PENDING y semántica exacta de “futura”.
