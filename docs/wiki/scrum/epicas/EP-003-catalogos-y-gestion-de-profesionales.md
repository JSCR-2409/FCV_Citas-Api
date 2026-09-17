---
id: EP-003
tipo: epica
titulo: "Catálogos y gestión de profesionales"
estado: Pendiente de aprobación
historias:
  - "[[HU-009-consultar-catalogos-fijos]]"
  - "[[HU-010-administrar-eps]]"
  - "[[HU-011-administrar-planes-eps]]"
  - "[[HU-012-administrar-especialidades]]"
  - "[[HU-013-crear-profesional]]"
  - "[[HU-014-asignar-especialidades-profesional]]"
  - "[[HU-015-asignar-sedes-y-estado-profesional]]"
dependencias:
  - "[[HU-001-modelar-datos-3fn]]"
  - "[[HU-002-documentar-contrato-rest-inicial]]"
  - "[[HU-004-iniciar-sesion-jwt]]"
---

# EP-003 — Catálogos y gestión de profesionales

## Objetivo

Proveer datos maestros controlados y profesionales habilitados para publicar y atender disponibilidad.

## Valor esperado

Permite configurar la oferta asistencial sintética sin comprometer transacciones existentes.

## Actores

- ADMIN.
- USER como consumidor de catálogos fijos.

## Alcance

- Catálogos fijos de solo lectura; CRUD de EPS, planes y especialidades; creación, especialidades, sedes y estado del PROFESSIONAL.

## Fuera de alcance

- Borrado físico de datos maestros referenciados; datos reales de FCV.

## Reglas de negocio

- Sedes/roles/estados/régimen fijos; especialidad 30 o 60 min; profesional con una especialidad primaria, una o ambas sedes y estado activo/inactivo.

## Dependencias

- [[HU-001-modelar-datos-3fn]]
- [[HU-002-documentar-contrato-rest-inicial]]
- [[HU-004-iniciar-sesion-jwt]]

## Historias de usuario

- [[HU-009-consultar-catalogos-fijos]]
- [[HU-010-administrar-eps]]
- [[HU-011-administrar-planes-eps]]
- [[HU-012-administrar-especialidades]]
- [[HU-013-crear-profesional]]
- [[HU-014-asignar-especialidades-profesional]]
- [[HU-015-asignar-sedes-y-estado-profesional]]

## Criterio de completitud de la épica

- [ ] Las siete HU están `Completada` y se conservan las restricciones de catálogos/transacciones.
- [ ] Existen profesionales activos configurables para disponibilidad.

## Riesgos e incógnitas

- La representación exacta de desactivación y sus efectos en disponibilidad futura requieren contrato aprobado.
