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

Las 34 HU se crearon en `Pendiente de aprobación`. S2, S3 y S4 deben seleccionar explícitamente una HU, respetar sus dependencias y actualizar su evidencia de validación.

Estado del backlog al 2026-10-04, tras cerrar el alcance de S2 a S6:

| Estado | HU | Criterio |
|---|---|---|
| `Completada` | **las 34** | CA verificados con pruebas automatizadas y DoD cumplida, incluida la UI correspondiente |

**Ninguna HU queda `Pendiente de aprobación`.** HU-001 a HU-005, el alcance de S2, estaban
implementadas desde `f299ec9` sin evidencia registrada; se regularizaron al cerrar S6. Al hacerlo
apareció un hueco real: **no existía ninguna prueba de `POST /api/auth/logout`**, el único endpoint
cuya razón de ser es revocar el refresh. Se añadieron dos, de modo que el CA-02 de HU-005 pasó a tener
respaldo en lugar de darse por bueno.

**S3** cerró la agenda y la reserva: administración de profesionales y asignaciones, gestión de
bloques, consulta de disponibilidad, cita general auto-aprobada, cita especializada en `REQUESTED` y
resolución por el ADMIN, cada una con su UI.

**S4** cerró el MVP: recuperación de contraseña con token de un solo uso, perfil, afiliación a EPS y
plan, catálogo configurable de EPS y planes, mis citas con filtros y motivo de rechazo, cancelación
con historial, agenda del profesional, cierre de atención y auditoría de cambios de estado. La
evidencia de los ciclos Builder/Verifier vive en [`docs/evidencia/loops/`](../../evidencia/loops/).

**S5 y S6** añadieron las automatizaciones sin tocar el núcleo: tres workflows n8n construidos por
MCP y versionados como JSON sin credenciales, un token de servicio con autoridad propia y un webhook
de salida asíncrono. La evidencia está en [`docs/evidencia/mcp-n8n.md`](../../evidencia/mcp-n8n.md) y
los riesgos residuales en
[`docs/evidencia/seguridad-contenido-no-confiable.md`](../../evidencia/seguridad-contenido-no-confiable.md).

La evidencia de cada HU vive en su propia tabla **Evidencia de validación**, con el nombre de la
prueba que respalda cada criterio. La suite de backend es de **166 pruebas** y está en verde; la de
frontend, de 8.

HU-024, HU-029 y HU-030 pertenecen a S4, pero se abordaron en S3 al detectar que la reprogramación no
existía en ninguna capa. Cada una se validó con sus propias pruebas, conforme a la regla de
`PLAN_AJUSTADO_S3_S5.md` de no fusionar las tres en un mismo bloque de verificación.

### Cómo se cerraron HU-032 y HU-034

Su CA-01 dice «cuando corre el workflow», y durante un tiempo quedaron `En validación` porque la
instancia de n8n es en la nube y la API corre en `localhost:8080`. Se cerraron abriendo un **túnel
efímero** hacia la API, ejecutando los dos workflows contra datos reales y cerrándolo: el túnel estuvo
abierto unos tres minutos y queda registrado en
[`seguridad-contenido-no-confiable.md`](../../evidencia/seguridad-contenido-no-confiable.md), apartado
4.6-bis.

Ninguno de los dos CA-01 exige enviar correo: hablan de **seleccionar** y de **agregar**. Por eso la
ausencia de credencial de Gmail no los bloqueaba, y por eso las copias de prueba omitieron el nodo
Gmail: no participa en lo que el criterio pide.

El cierre del túnel se aprovechó para probar el requisito de WF-001 de «manejar API no disponible»,
que de otro modo se habría quedado sin evidencia: tres reintentos y `API_UNAVAILABLE`.

**Lo único que sigue sin probarse es el envío de un correo**, que depende de la credencial de Google
Cloud de cada estudiante.
