---
id: HU-030
tipo: historia-de-usuario
titulo: "Resolver reprogramación"
estado: Pendiente de aprobación
epica: "[[EP-007-operacion-administrativa-y-auditoria]]"
esfuerzo: Muy alto
sprint_sugerido: "Incremento V — Operación clínica simulada"
dependencias: ["[[HU-029-consultar-bandeja-reprogramaciones]]"]
relacionadas: ["[[HU-023-cancelar-cita]]", "[[HU-031-auditar-cambios-de-estado]]"]
---

# HU-030 — Resolver reprogramación

## Historia de usuario
**COMO** ADMIN **QUIERO** aprobar o rechazar una reprogramación PENDING **PARA** mover una cita solo cuando corresponda y mantenerla si rechazo la propuesta.

## Alcance
- Aprobar: libera slots antiguos, asigna nuevos y actualiza cita; rechazar: libera propuesta y conserva original; motivo cuando corresponda; auditoría.

## Fuera de alcance
- Cambiar profesional/especialidad, destruir original al rechazar o impedir que USER cancele tras rechazo.

## Reglas de negocio
- Original se conserva hasta decisión; aprobación mueve; rechazo conserva original/libera propuesta; USER puede conservar o cancelar tras rechazo.

## Dependencias y relaciones
- Épica: [[EP-007-operacion-administrativa-y-auditoria]]
- Dependencias: [[HU-029-consultar-bandeja-reprogramaciones]]
- Relacionadas: [[HU-023-cancelar-cita]], [[HU-031-auditar-cambios-de-estado]]

## Esfuerzo
**Nivel:** Muy alto. Es una transición coordinada de dos franjas y dos objetos con riesgo transaccional; se mantiene separada de la solicitud.

## Tareas de desarrollo
- [ ] **T-01 — Precisar estados, motivo y efectos de decisión.** Dificultad: Alto. Registrar reglas faltantes explícitamente.
- [ ] **T-02 — Implementar aprobación/rechazo atómico con slots e historial.** Dificultad: Alto. Evitar pérdida/doble reserva.
- [ ] **T-03 — Integrar decisión ADMIN, actualización USER y pruebas.** Dificultad: Alto. Comprobar las dos ramas de resultado.

## Criterios de aceptación
### CA-01 — Aprobación mueve la cita
Dado una reprogramación PENDING válida, cuando ADMIN aprueba, entonces se liberan slots antiguos, se asignan nuevos y la cita queda actualizada.
### CA-02 — Rechazo preserva original
Dado una reprogramación PENDING, cuando ADMIN rechaza con el motivo aplicable, entonces se libera propuesta, se mantiene la cita original y USER puede conservarla o cancelarla.
### CA-03 — Consistencia ante conflicto
Dado una solicitud ya resuelta o una propuesta que no puede confirmarse según regla aprobada, cuando se intenta decidir, entonces no se producen cambios parciales y se informa resultado conforme a contrato.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas transaccionales, concurrencia, estados, RBAC y frontend aplicable.
- [ ] Las actualizaciones de cita/slots/solicitud/auditoría son atómicas y cuentan con Flyway/índices si aplica.
- [ ] Contrato y UI comunican aprobación, rechazo, motivo y opción posterior de cancelación sin BFF.
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
- PREGUNTA ABIERTA: qué representa “motivo cuando corresponda” en la decisión de reprogramación y cómo resolver colisión al aprobar.
