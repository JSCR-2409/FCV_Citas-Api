---
id: EP-004
tipo: epica
titulo: "Disponibilidad y búsqueda de horarios"
estado: Pendiente de aprobación
historias:
  - "[[HU-016-crear-bloques-de-disponibilidad]]"
  - "[[HU-017-modificar-bloques-futuros]]"
  - "[[HU-018-consultar-calendario-profesional]]"
  - "[[HU-019-consultar-disponibilidad]]"
dependencias:
  - "[[HU-012-administrar-especialidades]]"
  - "[[HU-015-asignar-sedes-y-estado-profesional]]"
---

# EP-004 — Disponibilidad y búsqueda de horarios

## Objetivo

Permitir que el PROFESSIONAL publique una agenda válida y que USER encuentre franjas completas reservables.

## Valor esperado

Construye una oferta de horarios coherente para los flujos de cita sin doble reserva.

## Actores

- PROFESSIONAL.
- USER.

## Alcance

- Crear, editar/eliminar bloques futuros válidos, ver calendario y filtrar disponibilidad.

## Fuera de alcance

- Crear bloques en pasado, solapados, en sede no asignada o editar bloques comprometidos.

## Reglas de negocio

- Slots de 30 min; duración de especialidad 30/60 min; 60 min requiere dos slots consecutivos; sede asignada y especialidad activa/asociada.

## Dependencias

- [[HU-012-administrar-especialidades]]
- [[HU-015-asignar-sedes-y-estado-profesional]]

## Historias de usuario

- [[HU-016-crear-bloques-de-disponibilidad]]
- [[HU-017-modificar-bloques-futuros]]
- [[HU-018-consultar-calendario-profesional]]
- [[HU-019-consultar-disponibilidad]]

## Criterio de completitud de la épica

- [ ] Las cuatro HU están `Completada` con validación de franjas y permisos.
- [ ] Los resultados de disponibilidad solo contienen franjas completas elegibles.

## Riesgos e incógnitas

- Semántica de concurrencia frente a confirmaciones simultáneas debe quedar aprobada en el contrato.
