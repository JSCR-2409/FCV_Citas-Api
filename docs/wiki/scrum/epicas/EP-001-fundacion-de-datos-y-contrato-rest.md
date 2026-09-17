---
id: EP-001
tipo: epica
titulo: "Fundación de datos y contrato REST"
estado: Pendiente de aprobación
historias:
  - "[[HU-001-modelar-datos-3fn]]"
  - "[[HU-002-documentar-contrato-rest-inicial]]"
dependencias: []
---

# EP-001 — Fundación de datos y contrato REST

## Objetivo

Definir la base relacional 3FN y el contrato REST documentado que permitan construir cortes funcionales consistentes entre frontend y backend.

## Valor esperado

Reduce ambigüedad de datos e integración antes de construir reservas, seguridad y pantallas.

## Actores

- Equipo de producto y desarrollo.

## Alcance

- Modelo y justificación 1FN–3FN, claves, cardinalidades, catálogos, reservas y auditoría.
- Contrato REST trazable para las capacidades que se aprueben.

## Fuera de alcance

- Implementación de base de datos, migraciones, endpoints o UI.

## Reglas de negocio

- 3FN; relaciones N:M sin listas; catálogos no duplicados; soporte para reservas de 30/60 min y reprogramación sin destruir la cita original.

## Dependencias

- Ninguna.

## Historias de usuario

- [[HU-001-modelar-datos-3fn]]
- [[HU-002-documentar-contrato-rest-inicial]]

## Criterio de completitud de la épica

- [ ] Ambas HU están `Completada` con decisiones aprobadas y evidencia.
- [ ] Las HU posteriores pueden seleccionar datos y contrato sin contradicciones conocidas.

## Riesgos e incógnitas

- El contrato no está aprobado y el mecanismo transaccional de retenciones no está definido.
