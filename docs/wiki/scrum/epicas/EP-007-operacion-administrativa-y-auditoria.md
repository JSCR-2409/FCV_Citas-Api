---
id: EP-007
tipo: epica
titulo: "Operación administrativa y auditoría"
estado: Pendiente de aprobación
historias:
  - "[[HU-027-consultar-bandeja-especializada]]"
  - "[[HU-028-resolver-solicitud-especializada]]"
  - "[[HU-029-consultar-bandeja-reprogramaciones]]"
  - "[[HU-030-resolver-reprogramacion]]"
  - "[[HU-031-auditar-cambios-de-estado]]"
dependencias:
  - "[[HU-021-solicitar-cita-especializada]]"
  - "[[HU-024-solicitar-reprogramacion]]"
---

# EP-007 — Operación administrativa y auditoría

## Objetivo

Permitir que ADMIN gestione solicitudes pendientes y que todo cambio de estado sea rastreable.

## Valor esperado

Convierte solicitudes retenidas en resultados explícitos y auditables.

## Actores

- ADMIN.

## Alcance

- Bandejas filtrables, aprobación/rechazo de solicitud especializada y reprogramación, y consulta de historial de estados.

## Fuera de alcance

- CRUD normal sobre registros de auditoría y aprobación de citas generales.

## Reglas de negocio

- Rechazo con motivo; aprobar/rechazar libera/asigna slots correctos; auditoría contiene cita, nuevo estado, actor cuando exista, fuente, fecha/hora y motivo opcional.

## Dependencias

- [[HU-021-solicitar-cita-especializada]]
- [[HU-024-solicitar-reprogramacion]]

## Historias de usuario

- [[HU-027-consultar-bandeja-especializada]]
- [[HU-028-resolver-solicitud-especializada]]
- [[HU-029-consultar-bandeja-reprogramaciones]]
- [[HU-030-resolver-reprogramacion]]
- [[HU-031-auditar-cambios-de-estado]]

## Criterio de completitud de la épica

- [ ] Las cinco HU están `Completada` y cada decisión deja resultado y auditoría verificables.
- [ ] Las bandejas muestran solo el estado pendiente que les corresponde.

## Riesgos e incógnitas

- Se requiere acordar el modelo de lectura/paginación de las bandejas en HU-002.
