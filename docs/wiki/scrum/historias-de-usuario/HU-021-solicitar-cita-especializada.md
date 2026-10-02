---
id: HU-021
tipo: historia-de-usuario
titulo: "Solicitar cita especializada"
estado: Completada
epica: "[[EP-005-ciclo-de-citas-del-usuario]]"
esfuerzo: Alto
sprint_sugerido: "Incremento IV — Reserva del usuario"
dependencias: ["[[HU-019-consultar-disponibilidad]]"]
relacionadas: ["[[HU-027-consultar-bandeja-especializada]]", "[[HU-028-resolver-solicitud-especializada]]"]
---

# HU-021 — Solicitar cita especializada

## Historia de usuario
**COMO** USER autenticado **QUIERO** solicitar una cita de especialidad con sede, profesional y horario **PARA** que ADMIN la resuelva sin perder la franja seleccionada.

## Alcance
- Crear solicitud especializada en REQUESTED y retener los slots requeridos.

## Fuera de alcance
- Aprobación automática, eliminar la retención antes de una decisión o solicitud para especialidad/profesional no elegible.

## Reglas de negocio
- Nace REQUESTED; horario retenido; rechazo libera slots y exige motivo; 60 min requiere slots consecutivos.

## Dependencias y relaciones
- Épica: [[EP-005-ciclo-de-citas-del-usuario]]
- Dependencias: [[HU-019-consultar-disponibilidad]]
- Relacionadas: [[HU-027-consultar-bandeja-especializada]], [[HU-028-resolver-solicitud-especializada]]

## Esfuerzo
**Nivel:** Alto. Integra elegibilidad, retención, transacción, cola administrativa y auditoría.

## Tareas de desarrollo
- [x] **T-01 — Definir solicitud REQUESTED y modelo de retención.** Dificultad: Alto. Registrar expiración pendiente.
- [x] **T-02 — Implementar creación atómica con exclusión de slots.** Dificultad: Alto. Validar todas las asociaciones.
- [x] **T-03 — Integrar confirmación USER y pruebas de cola ADMIN.** Dificultad: Alto. Informar conflicto sin datos sensibles.

## Criterios de aceptación
### CA-01 — Solicitud retenida
Dado una franja especializada elegible libre, cuando USER confirma, entonces se crea REQUESTED y los slots quedan retenidos.
### CA-02 — Sin doble reserva
Dado un slot ya reservado o retenido, cuando otro USER intenta solicitarlo, entonces se rechaza y no se crea solicitud solapada.
### CA-03 — Datos de decisión disponibles
Dado una solicitud creada, cuando ADMIN consulta su bandeja, entonces dispone de sede, profesional, especialidad, fecha/hora y duración para decidir.

## Definition of Done
- [x] CA-01 a CA-03 tienen pruebas de concurrencia, duración 30/60, persistencia/REST y UI aplicable.
- [x] Retenciones y solicitud tienen Flyway/índices transaccionales cuando se agregan.
- [x] El contrato describe respuesta a conflicto y la evidencia cross-repo confirma su consumo.
- [x] La trazabilidad Scrum está actualizada.

## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumplido | AppointmentBookingRulesTest.hu021_ca01_specializedIsRequestedAndHoldsTheSlot y hu021_ca01_specializedEndpointRejectsAGeneralSpecialty | Verificado el 2026-10-02 |
| CA-02 | Cumplido | AppointmentBookingRulesTest.hu021_ca02_requestOnAHeldSlotIsRejected | Verificado el 2026-10-02 |
| CA-03 | Cumplido | SpecializedRequestDecisionTest.hu021_ca03_trayCarriesTheDataNeededToDecide; la bandeja entrega sede, profesional, especialidad, franja y duracion | Verificado el 2026-10-02 |
| DoD | Cumplida: pruebas REST, contrato 1.1 y UI de solicitud en el portal del paciente | Suite de 59 pruebas de backend en verde | Revisado el 2026-10-02 |

## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-02 — Implementacion verificada contra la suite de pruebas y el entorno MySQL; HU pasa a `Completada`.

## Notas y decisiones
- PREGUNTA ABIERTA: caducidad/limpieza de una solicitud REQUESTED no resuelta.
