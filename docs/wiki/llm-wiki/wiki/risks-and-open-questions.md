# Riesgos y preguntas abiertas

Actualizado: 2026-10-04.

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

---

# Añadidos en S4, S5 y S6 — 2026-10-04

## RESUELTO — Exposición del token de recuperación y efecto en el refresh activo

Era PREGUNTA ABIERTA de HU-006. Las tres decisiones: el token viaja en la respuesta solo si
`app.recovery.expose-token` es `true`, cuyo valor por defecto es `false`; consumirlo revoca todas las
sesiones vivas de la cuenta; y la solicitud responde igual exista o no la cuenta, para no convertir
el endpoint en un oráculo de enumeración. Detalle en `docs/contratos/auth-rest.md` 1.2.

## RESUELTO — Matriz de campos editables del perfil

Era PREGUNTA ABIERTA de HU-007. Editables: `names`, `surnames` y `phone`. Fuera: documento y email,
que identifican la cuenta; `active` y roles, que son decisiones administrativas.

## RESUELTO — Orden, paginación y filtro de fecha de «mis citas»

Era PREGUNTA ABIERTA de HU-022. Orden descendente, sin paginación, y filtro de fecha inclusivo en
ambos extremos sobre el día completo.

## RESUELTO — Estados terminales para efectos de cancelación

Era PREGUNTA ABIERTA de HU-023. Cancelables: `APPROVED` y `REQUESTED`. Terminales: `CANCELLED`,
`REJECTED`, `COMPLETED` y `NO_SHOW`.

## RIESGO — El token de integración es estático y no rota

`INTEGRATION_TOKEN` no caduca ni se renueva. Si se filtra, concede lectura de los datos de citas de
la ventana consultable hasta que alguien lo cambie a mano. Mitigación parcial: es de solo lectura y
tiene autoridad propia, de modo que no abre los endpoints administrativos. Falta rotación, caducidad
y registro de uso.

## RIESGO — La firma HMAC del webhook se emite pero nadie la verifica

El backend firma el payload en `X-Signature`. WF-002 **no comprueba esa firma**: se apoya en el Header
Auth del webhook. Mientras siga así, la firma es una capa preparada pero inactiva, y describir el
webhook como «firmado» sería engañoso.

## RIESGO — El contenido del correo de WF-002 no está escapado

`patientName`, `specialtyName` y `reason` se insertan en HTML sin escapar. El `reason` lo escribe un
ADMIN y los nombres vienen del registro, así que el riesgo no es un atacante anónimo, pero tampoco es
cero.

## RIESGO — Sin límite de peticiones en integración ni en recuperación

Nada impide probar el token de integración en bucle, ni pedir recuperación de contraseña de forma
masiva. Lo segundo permitiría generar tokens en cantidad, aunque no leerlos.

## RIESGO — El canal del token de recuperación es de laboratorio

`app.recovery.expose-token=true` es una decisión de configuración, no una barrera de código. Si
alguien lo habilitara en un entorno real, cualquiera que conozca un correo registrado podría tomar la
cuenta.

## PREGUNTA ABIERTA — n8n no puede alcanzar el backend local

La instancia de n8n es en la nube y la API corre en `localhost:8080`. Los tres workflows están
construidos, versionados y validados, pero la cadena completa hasta un correo real **no se ejecutó**,
y no por diseño sino por topología. Por eso HU-032 y HU-034 quedan `En validación` en lugar de
`Completada`: su CA-01 dice «cuando corre el workflow». Resolverlo exige exponer o desplegar la API.

## RIESGO — No hay credenciales en la instancia de n8n

`list_credentials` devuelve lista vacía: no existe credencial de Gmail OAuth2. Es lo que
`GUIA_SESIONES_S2_S6.md` pide que configure cada estudiante con su propia cuenta de Google Cloud, y
no puede hacerse desde el agente. Sin ella, ningún nodo Gmail de los tres workflows envía nada.

El análisis completo de contenido no confiable y los ocho riesgos residuales están en
`docs/evidencia/seguridad-contenido-no-confiable.md`.
