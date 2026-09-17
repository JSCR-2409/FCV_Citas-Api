---
id: EP-002
tipo: epica
titulo: "Identidad, sesión y perfil"
estado: Pendiente de aprobación
historias:
  - "[[HU-003-registrar-usuario]]"
  - "[[HU-004-iniciar-sesion-jwt]]"
  - "[[HU-005-renovar-y-cerrar-sesion]]"
  - "[[HU-006-recuperar-contrasena]]"
  - "[[HU-007-consultar-y-actualizar-perfil]]"
  - "[[HU-008-gestionar-afiliacion]]"
dependencias:
  - "[[HU-001-modelar-datos-3fn]]"
  - "[[HU-002-documentar-contrato-rest-inicial]]"
---

# EP-002 — Identidad, sesión y perfil

## Objetivo

Permitir que un USER ficticio tenga una cuenta segura, sesión controlada y perfil/afiliación administrables.

## Valor esperado

Habilita acceso seguro y los datos necesarios para solicitar y consultar citas.

## Actores

- USER.

## Alcance

- Registro, JWT access/refresh, logout, recuperación de contraseña, perfil y afiliación EPS/plan/régimen.

## Fuera de alcance

- Identidad clínica real, autenticación externa y envío SMTP obligatorio.

## Reglas de negocio

- Email/documento únicos; contraseña con hash adaptativo; tokens separados; reset temporal de un solo uso; no duplicar EPS/régimen/plan en USER.

## Dependencias

- [[HU-001-modelar-datos-3fn]]
- [[HU-002-documentar-contrato-rest-inicial]]

## Historias de usuario

- [[HU-003-registrar-usuario]]
- [[HU-004-iniciar-sesion-jwt]]
- [[HU-005-renovar-y-cerrar-sesion]]
- [[HU-006-recuperar-contrasena]]
- [[HU-007-consultar-y-actualizar-perfil]]
- [[HU-008-gestionar-afiliacion]]

## Criterio de completitud de la épica

- [ ] Las seis HU están `Completada` y sus controles de seguridad aplicables tienen evidencia.
- [ ] Un USER puede llegar autenticado a las capacidades autorizadas sin exponer secretos.

## Riesgos e incógnitas

- Forma controlada de exponer o entregar el token de recuperación en desarrollo.
