---
id: HU-023
tipo: historia-de-usuario
titulo: "Cancelar cita"
estado: Completada
epica: "[[EP-005-ciclo-de-citas-del-usuario]]"
esfuerzo: Alto
sprint_sugerido: "Incremento IV — Reserva del usuario"
dependencias: ["[[HU-022-consultar-mis-citas]]"]
relacionadas: ["[[HU-031-auditar-cambios-de-estado]]"]
---

# HU-023 — Cancelar cita

## Historia de usuario
**COMO** USER autenticado **QUIERO** cancelar una cita futura no terminal propia **PARA** liberar su horario sin reactivar una cita cancelada.

## Alcance
- Transición a CANCELLED, liberación de slots e historial de cambio.

## Fuera de alcance
- Cancelar cita ajena/pasada/terminal, reactivar directamente CANCELLED o borrar la cita.

## Reglas de negocio
- Solo futura no terminal; CANCELLED libera slots; no reactivación directa; historial obligatorio.

## Dependencias y relaciones
- Épica: [[EP-005-ciclo-de-citas-del-usuario]]
- Dependencias: [[HU-022-consultar-mis-citas]]
- Relacionada: [[HU-031-auditar-cambios-de-estado]]

## Esfuerzo
**Nivel:** Alto. Cambia estado, reserva, visibilidad y auditoría de forma consistente.

## Tareas de desarrollo
- [ ] **T-01 — Definir estados terminales y transición autorizada.** Dificultad: Alto. No inferir estados no fijados.
- [ ] **T-02 — Implementar cancelación atómica con liberación/auditoría.** Dificultad: Alto. Proteger ownership.
- [ ] **T-03 — Integrar acción UI y pruebas de franja liberada.** Dificultad: Medio. Gestionar conflicto de estado.

## Criterios de aceptación
### CA-01 — Cancelación permitida
Dado una cita propia futura no terminal, cuando USER cancela, entonces queda CANCELLED y sus slots se liberan.
### CA-02 — Límites de cancelación
Dado una cita ajena, pasada o terminal, cuando USER intenta cancelar, entonces se rechaza y no cambia.
### CA-03 — Historial y no reactivación
Dado una cancelación exitosa, cuando se revisa historial/operación posterior, entonces se registra y no puede reactivarse directamente.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de transición, ownership, liberación y auditoría.
- [ ] La actualización de cita/slots/historial es consistente, con migración Flyway si se agrega esquema.
- [ ] Cliente refleja resultado/conflicto desde contrato REST sin asumir autorización local.
- [ ] La trazabilidad Scrum está actualizada.

## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumplido | ProfileAndMyAppointmentsTest.hu023_ca01_* | CANCELLED es terminal y libera las franjas |
| CA-02 | Cumplido | hu023_ca02_* (3) | CANCELLED es terminal y libera las franjas |
| CA-03 | Cumplido | hu023_ca03_aCancelledAppointmentCannotBeCancelledAgain | CANCELLED es terminal y libera las franjas |
| DoD | Cumplido | 5 pruebas; historial verificado en la misma prueba | 4/4 |

## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-02 — S4: CA verificados con pruebas automatizadas y UI conectada; estado `Completada`.

## Notas y decisiones

- ~~PREGUNTA ABIERTA: conjunto exacto de estados terminales para efectos de cancelación.~~
  **Resuelta el 2026-10-02 (S4).**

- **DECISIÓN — estados cancelables.** Solo `APPROVED` y `REQUESTED`. Terminales, y por tanto no
  cancelables: `CANCELLED`, `REJECTED`, `COMPLETED` y `NO_SHOW`. `COMPLETED` y `NO_SHOW` describen una
  atención que ya ocurrió, y cancelarla reescribiría un hecho; `REJECTED` y `CANCELLED` ya liberaron
  sus franjas. Verificado en `hu023_ca02_aCompletedAppointmentIsTerminalForTheUser` y
  `hu023_ca03_aCancelledAppointmentCannotBeCancelledAgain`.

- Las tres condiciones —cita propia, futura y en estado cancelable— van en la guarda del mismo
  `UPDATE`, no comprobadas por separado: así no hay ventana entre la lectura y la escritura. El
  `0` filas afectadas se traduce a `409`.

- Cancelar la cita cierra también la reprogramación pendiente que tuviera, porque el ADMIN ya no
  tiene que decidir sobre una cita que no existe.
