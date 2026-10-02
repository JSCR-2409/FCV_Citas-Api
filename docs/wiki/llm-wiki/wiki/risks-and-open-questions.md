# Riesgos y preguntas abiertas

Actualizado: 2026-10-02.

## PREGUNTA ABIERTA — Código de estado para peticiones no autenticadas

`docs/contratos/catalogos-fijos-rest.md` fija que sin autenticación la respuesta es `403` «según la
configuración actual de Spring Security». Para una API con JWT lo habitual sería `401` para no
autenticado y `403` para autenticado sin permiso. El comportamiento actual coincide con el contrato,
así que **no se ha cambiado**; la prueba `CatalogAndAuthorizationTest` verifica el `403` contratado.
Resolverlo exige decidir si se corrige el contrato o la configuración.

## PREGUNTA ABIERTA — `400` frente a `403` al crear bloque en sede no asignada

`docs/contratos/bloques-disponibilidad-rest.md` indica `403` «por sede no asignada/profesional
inactivo». El código devuelve `400` para sede no asignada y `403` para profesional inactivo. HU-015
CA-02 solo exige que se rechace, sin fijar el código. Divergencia documentada, sin resolver por
inferencia.

## PREGUNTA ABIERTA — Concurrencia entre lectura de disponibilidad y confirmación

Heredada de HU-019. El reclamo de franjas es un único `UPDATE` con guarda `appointment_id IS NULL`,
y si el número de slots reclamados no coincide con el esperado la transacción revierte. No hay
retención con TTL: una franja consultada puede dejar de estar libre antes de confirmar, y el usuario
recibe `409`. Falta decidir si se requiere reserva temporal explícita.

## RIESGO — El esquema vigente depende de `database/reference/db.sql`

La BD de desarrollo se inicializa con ese archivo como init script de MySQL, y Flyway queda en modo
baseline. `README.md` advierte que el trainer puede ocultar temporalmente esa carpeta, y el
`.gitignore` de la raíz lista `db.sql`. Mitigación aplicada: las migraciones `V7` y `V8` crean las
tablas y los estados que faltaban, de modo que un entorno nuevo puede levantarse solo con Flyway.

## RIESGO — Divergencia deliberada `refresh_sessions` / `refresh_tokens`

El modelo de referencia define `refresh_tokens`; el código usa `refresh_sessions`, creada en `V2`.
Se mantiene `refresh_sessions` como decisión registrada. `V7` no crea `refresh_tokens` para no
dejar esquema muerto, así que un entorno solo-Flyway no tendrá esa tabla.

## RIESGO — La arquitectura no es hexagonal

`RESTRICCIONES_TECNICAS.md` la exige. La implementación son paquetes planos por feature con SQL
embebido en los controladores mediante `JdbcTemplate`. No se ha abordado.

## Estado de verificación pendiente de S3

- No existe hook local de verificación: `core.hooksPath` sin configurar en ambos repos.
- No existe bloqueo de secretos (gitleaks, pre-commit o equivalente).
- El frontend no tiene pruebas de lógica de negocio: un único spec autogenerado.
- `package.json` del frontend no define script `typecheck`.
- No se registra `appointment_status_history`: corresponde a HU-031, aún sin aprobar.
