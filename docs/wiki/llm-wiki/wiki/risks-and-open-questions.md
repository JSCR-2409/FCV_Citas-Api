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

---

# Riesgos cerrados en el repaso posterior a S6 — 2026-10-04

## RESUELTO — La firma HMAC que nadie verificaba

No se resolvió verificando el HMAC, sino **cambiando el mecanismo**. Comprobar un HMAC en n8n exige
que un nodo Code tenga el secreto, y meterlo ahí lo dejaría dentro del JSON versionado: la defensa se
habría pagado rompiendo la regla de no versionar credenciales.

El webhook pasa a la autenticación **JWT nativa** de n8n. El backend emite un token HS256 de dos
minutos y n8n verifica firma y `exp` por sí mismo, con el secreto como credencial. El `X-Signature` se
retiró. No deja hueco: sin el secreto no se puede construir ninguna petición válida, y la integridad
del cuerpo en tránsito la cubre TLS.

## RESUELTO — El contenido del correo no estaba escapado

Un solo nodo escapa a entidades HTML los campos que van al cuerpo. Verificado con una carga hostil:
`<script>alert(1)</script>` sale como `&lt;script&gt;alert(1)&lt;/script&gt;`. El destinatario no se
escapa porque va al campo `sendTo`.

## RESUELTO — Sin límite de peticiones

`RateLimitFilter`: 5 por minuto en recuperación y 60 en integración, por IP, con `429` y
`Retry-After`. Se registra **por delante de Spring Security**; en la primera versión iba después y el
`403` de un token inválido se adelantaba al contador, de modo que el límite no frenaba precisamente el
abuso que debe cortar.

## RESUELTO — Un secreto corto desactivaba el webhook en silencio

HS256 exige 256 bits. Con un secreto más corto, cada notificación lanzaba `WeakKeyException` y el
webhook no funcionaba nunca; lo único que lo delataba era un aviso por cita. Ahora la longitud se
valida al arrancar y el notificador queda desactivado con un `ERROR` explícito.

## RESUELTO — El mapa de especialidades codificado en el frontend

El formulario de reserva traducía nombres a identificadores con un objeto literal y nunca consultaba
`/catalogs/specialties`. Tres consecuencias: una especialidad nueva del ADMIN no se podía reservar, un
cambio de identificadores reservaba la especialidad equivocada en silencio, y la condición
`specialtyId === 1` enviaba cualquier especialidad general distinta de la primera al endpoint de
especializadas. Ahora el selector se puebla del catálogo y el endpoint lo decide el flag `general`.

De paso, el selector de sede dejó de ser decorativo: su valor llega a la consulta de disponibilidad.

## RESUELTO — El token de integración es fijo y, ahora sí, rotable

La decisión: **sigue fijo**, pero `app.integrations.token` admite varios valores separados por coma, el
primero vigente y los siguientes en retirada. El problema de un token fijo no era su duración sino que
cambiarlo obligaba a elegir entre dejar la automatización caída o no cambiarlo nunca. Cada uso correcto
se registra con su IP: antes un token filtrado no dejaba ningún rastro.

Descartadas una tabla de tokens con revocación individual, porque hay un único consumidor, y una
caducidad fija, porque un token que expira solo rompe una automatización desatendida en un momento que
nadie eligió.

Probado sobre una filtración real: el token que hubo que escribir en los workflows de prueba se retiró
sin interrupción.

## RESUELTO — n8n no alcanzaba el backend local

Se cerró con un túnel efímero de unos tres minutos, usado solo para ejecutar WF-001 y WF-003 contra
datos reales. HU-032 y HU-034 pasaron a `Completada`, y el backlog quedó con **las 34 HU completas**.

Los dos CA-01 hablan de **seleccionar** y de **agregar**, no de enviar, así que la credencial de Gmail
nunca los bloqueaba. Lo único que sigue sin probarse es el envío real de correo, que depende de la
credencial de Google Cloud de cada estudiante.

## VIGENTE — Lo que queda realmente abierto

| Asunto | Por qué sigue abierto |
|---|---|
| El canal del token de recuperación es de laboratorio | Admitido por RF-03; es configuración, no barrera de código |
| Sin credencial de Gmail en la instancia | La crea cada estudiante con su cuenta de Google Cloud |
| El `401` frente al `403` del contrato de catálogos | Exige decidir si se corrige el contrato o la configuración |
| El `400` frente al `403` en sede no asignada | HU-015 CA-02 no fija el código |
| Semántica de concurrencia en la retención de franjas | Falta decidir si se requiere reserva temporal explícita |
