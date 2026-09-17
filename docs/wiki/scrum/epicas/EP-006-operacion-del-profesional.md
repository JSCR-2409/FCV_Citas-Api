---
id: EP-006
tipo: epica
titulo: "Operación del profesional"
estado: Pendiente de aprobación
historias:
  - "[[HU-025-consultar-agenda-profesional]]"
  - "[[HU-026-cerrar-atencion]]"
dependencias:
  - "[[HU-020-reservar-cita-general]]"
  - "[[HU-021-solicitar-cita-especializada]]"
---

# EP-006 — Operación del profesional

## Objetivo

Dar al PROFESSIONAL una vista de sus citas aprobadas y el cierre de atención aplicable.

## Valor esperado

Permite operar la agenda sintética sin exponer datos ajenos.

## Actores

- PROFESSIONAL.

## Alcance

- Consulta por día/semana/sede de sus propias citas APPROVED; transición a COMPLETED o NO_SHOW.

## Fuera de alcance

- Consulta de pacientes ajenos, diagnósticos o historia clínica.

## Reglas de negocio

- Ownership estricto; solo citas pasadas/aplicables; transición auditada.

## Dependencias

- [[HU-020-reservar-cita-general]]
- [[HU-021-solicitar-cita-especializada]]

## Historias de usuario

- [[HU-025-consultar-agenda-profesional]]
- [[HU-026-cerrar-atencion]]

## Criterio de completitud de la épica

- [ ] Las dos HU están `Completada` con evidencia de ownership y auditoría.
- [ ] No se exponen datos de USER fuera de las citas propias del profesional.

## Riesgos e incógnitas

- El criterio operativo exacto de “pasada/aplicable” debe documentarse en contrato.
