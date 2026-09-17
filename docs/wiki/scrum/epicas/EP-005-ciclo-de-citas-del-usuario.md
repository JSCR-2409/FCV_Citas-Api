---
id: EP-005
tipo: epica
titulo: "Ciclo de citas del usuario"
estado: Pendiente de aprobación
historias:
  - "[[HU-020-reservar-cita-general]]"
  - "[[HU-021-solicitar-cita-especializada]]"
  - "[[HU-022-consultar-mis-citas]]"
  - "[[HU-023-cancelar-cita]]"
  - "[[HU-024-solicitar-reprogramacion]]"
dependencias:
  - "[[HU-019-consultar-disponibilidad]]"
---

# EP-005 — Ciclo de citas del usuario

## Objetivo

Permitir al USER reservar, consultar, cancelar y pedir reprogramación de citas según sus reglas de estado.

## Valor esperado

Entrega el flujo central de agendamiento con protección contra pérdidas y doble reserva.

## Actores

- USER.

## Alcance

- Cita general automática, solicitud especializada, mis citas, cancelación y solicitud de reprogramación.

## Fuera de alcance

- Reactivar una cancelada, cambiar profesional durante reprogramación, pagos e historia clínica.

## Reglas de negocio

- General se crea APPROVED; especializada nace REQUESTED y retiene slots; cancelación/rechazo libera; la original se preserva durante reprogramación PENDING.

## Dependencias

- [[HU-019-consultar-disponibilidad]]

## Historias de usuario

- [[HU-020-reservar-cita-general]]
- [[HU-021-solicitar-cita-especializada]]
- [[HU-022-consultar-mis-citas]]
- [[HU-023-cancelar-cita]]
- [[HU-024-solicitar-reprogramacion]]

## Criterio de completitud de la épica

- [ ] Las cinco HU están `Completada` y cada transición libera o conserva slots como corresponde.
- [ ] USER solo ve y opera sus propias citas.

## Riesgos e incógnitas

- Expiración de retenciones y precisión de estados terminales no están definidos por el PRD.
