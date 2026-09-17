---
tipo: indice-scrum
estado: Pendiente de aprobación
---

# Mapa Scrum / Spec-Driven Development — Sistema de Citas FCV

## Alcance y fuentes

Este mapa especifica el producto descrito exclusivamente en `PRD.md`, `RESTRICCIONES_TECNICAS.md` y `database/REQUISITOS_NORMALIZACION_3FN.md`. No implementa código ni define endpoints, DTO, tablas, componentes visuales o credenciales no aprobados.

Stack impuesto por las fuentes: backend Java 21 / Spring Boot 3.5.x / Maven / arquitectura hexagonal / JPA / MySQL 8.4 / Flyway / REST JSON / JWT; frontend TypeScript con React **o** Angular por decidir desde Stitch/AI Studio, sin BFF; n8n posterior. Todos los datos son sintéticos.

## Épicas

- [[EP-001-fundacion-de-datos-y-contrato-rest]]
- [[EP-002-identidad-sesion-y-perfil]]
- [[EP-003-catalogos-y-gestion-de-profesionales]]
- [[EP-004-disponibilidad-y-busqueda-de-horarios]]
- [[EP-005-ciclo-de-citas-del-usuario]]
- [[EP-006-operacion-del-profesional]]
- [[EP-007-operacion-administrativa-y-auditoria]]
- [[EP-008-automatizaciones-posteriores]]

## Propuesta de incrementos funcionales

| Incremento sugerido | Resultado demostrable | Historias en secuencia |
|---|---|---|
| I — Base especificable | Modelo 3FN y contrato REST trazables para el primer corte vertical. | HU-001 → HU-002 |
| II — Acceso y datos maestros | Usuario autenticable; catálogos y profesionales administrables. | HU-003 → HU-004 → HU-005 → HU-006 → HU-009 → HU-010 → HU-011 → HU-012 → HU-013 → HU-014 → HU-015 → HU-007 → HU-008 |
| III — Oferta de agenda | Profesional publica agenda válida y USER encuentra franjas reservables. | HU-016 → HU-017 → HU-018 → HU-019 |
| IV — Reserva del usuario | Citas generales y especializadas, consulta, cancelación y solicitud de reprogramación. | HU-020 → HU-021 → HU-022 → HU-023 → HU-024 |
| V — Operación clínica simulada | ADMIN resuelve las colas; PROFESSIONAL consulta/cierra atención; se cierra la cobertura de auditoría. | HU-027 → HU-028 → HU-029 → HU-030 → HU-025 → HU-026 → HU-031 |
| VI — Automatizaciones posteriores | Workflows n8n exportables sin alterar el núcleo. | HU-032 → HU-033 → HU-034 |

Los incrementos son orden de dependencia para una persona; no representan duración, capacidad ni estimación temporal. Dentro del incremento II, HU-007 puede adelantarse después de HU-004 si no se trabaja su afiliación hasta HU-010/HU-011. HU-031 se valida al final del incremento V porque su alcance cubre todas las transiciones construidas en los incrementos III–V.

## Trazabilidad funcional

| Fuente | HU trazables |
|---|---|
| RF-01 | [[HU-003-registrar-usuario]] |
| RF-02 | [[HU-004-iniciar-sesion-jwt]], [[HU-005-renovar-y-cerrar-sesion]] |
| RF-03 | [[HU-006-recuperar-contrasena]] |
| RF-04 | [[HU-007-consultar-y-actualizar-perfil]], [[HU-008-gestionar-afiliacion]] |
| RF-05 | [[HU-009-consultar-catalogos-fijos]] |
| RF-06 | [[HU-010-administrar-eps]], [[HU-011-administrar-planes-eps]], [[HU-012-administrar-especialidades]] |
| RF-07 | [[HU-013-crear-profesional]], [[HU-014-asignar-especialidades-profesional]], [[HU-015-asignar-sedes-y-estado-profesional]] |
| RF-08 | [[HU-016-crear-bloques-de-disponibilidad]], [[HU-017-modificar-bloques-futuros]], [[HU-018-consultar-calendario-profesional]] |
| RF-09 / RF-10 | [[HU-012-administrar-especialidades]], [[HU-019-consultar-disponibilidad]] |
| RF-11 / RF-12 | [[HU-020-reservar-cita-general]], [[HU-021-solicitar-cita-especializada]], [[HU-027-consultar-bandeja-especializada]], [[HU-028-resolver-solicitud-especializada]] |
| RF-13 / RF-14 | [[HU-022-consultar-mis-citas]], [[HU-023-cancelar-cita]] |
| RF-15 | [[HU-024-solicitar-reprogramacion]], [[HU-029-consultar-bandeja-reprogramaciones]], [[HU-030-resolver-reprogramacion]] |
| RF-16 / RF-17 | [[HU-025-consultar-agenda-profesional]], [[HU-026-cerrar-atencion]] |
| RF-18 / RF-19 | [[HU-027-consultar-bandeja-especializada]], [[HU-028-resolver-solicitud-especializada]], [[HU-029-consultar-bandeja-reprogramaciones]], [[HU-030-resolver-reprogramacion]], [[HU-031-auditar-cambios-de-estado]] |
| RF-20 y restricciones de integración | [[HU-002-documentar-contrato-rest-inicial]]; la DoD de cada HU cross-repo exige evidencia en ambos repositorios. |
| Requisitos 3FN | [[HU-001-modelar-datos-3fn]] y DoD de HU con persistencia. |
| Automatización posterior S5/S6 | [[HU-032-recordar-citas-proximas]], [[HU-033-notificar-cambios-de-estado]], [[HU-034-resumir-operacion-diaria]] |

Las RN-01 a RN-12 aparecen en las reglas, criterios y DoD de [[HU-016-crear-bloques-de-disponibilidad]] a [[HU-031-auditar-cambios-de-estado]]; las reglas transversales de seguridad se verifican en HU-003 a HU-008 y en todas las HU con autorización.

## Decisiones y preguntas abiertas

- **PREGUNTA ABIERTA:** el PRD exige diseñar y documentar REST, pero no aprueba endpoints, DTO, convenciones de error, paginación ni semántica exacta de concurrencia. HU-002 debe producir y someter ese contrato a aprobación antes de que las HU dependientes lo implementen.
- **PREGUNTA ABIERTA:** el frontend será React o Angular después del flujo Stitch → aprobación → AI Studio; las HU de UI deben adecuar sus tareas al framework realmente exportado.
- **PREGUNTA ABIERTA:** el PRD pide retener horarios especializados/reprogramados, pero no fija expiración, recuperación de retenciones interrumpidas ni estrategia transaccional concreta; debe resolverse en contrato/regla aprobada.
- **PREGUNTA ABIERTA:** recuperación por correo admite una respuesta o log controlado en desarrollo; el mecanismo seguro, exposición autorizada y ambiente aplicable requieren decisión antes de implementación.
- **PREGUNTA ABIERTA:** las automatizaciones n8n dependen de una instancia y credenciales del trainer. Solo los JSON sin credenciales pueden versionarse.

## Gobierno de estados

Las 34 HU se crean en `Pendiente de aprobación`. Ninguna está aprobada, en desarrollo, validación o completada. S2, S3 y S4 deben seleccionar explícitamente una HU aprobada, respetar sus dependencias y actualizar su evidencia de validación.
