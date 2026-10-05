# Contrato REST de identidad y sesión

Alcance: HU-003, HU-004 y HU-005. La API es REST directo; no expone contraseñas, hashes ni tokens en logs.

* `POST /api/auth/register` recibe `names`, `surnames`, `documentType`, `documentNumber`, `email`, `phone` y `password`. Devuelve `201` sin cuerpo. Email y pareja tipo/número de documento son únicos; conflicto devuelve `409`.
* `POST /api/auth/login` recibe `email` y `password`. Devuelve `200` con `accessToken`, `refreshToken`, `tokenType=Bearer`, `expiresIn`; credenciales inválidas devuelven `401` sin detalle sensible.
* `POST /api/auth/refresh` recibe `refreshToken`. Devuelve una nueva pareja de tokens y rota atómicamente el refresh anterior. Un refresh inválido, vencido o reutilizado devuelve `401`.
* `POST /api/auth/logout` recibe `refreshToken`, lo revoca y devuelve `204`.

El access token es JWT de corta duración (15 minutos por configuración); el refresh es JWT separado (7 días por configuración), almacenado únicamente como SHA-256 en `refresh_sessions`. Secretos y duración se configuran por variables de entorno, **sin valor por defecto**: si faltan, la aplicación no arranca.

## Roles en el token — versión 1.1

`user_roles` es una relación N:M y el PRD admite usuarios con varios roles, así que el token lleva
los dos claims:

| Claim | Contenido |
|---|---|
| `roles` | arreglo con **todos** los códigos de rol del usuario, en orden alfabético |
| `role` | el primero de `roles`; es el rol de presentación, no la fuente de autorización |

El filtro de autenticación concede una autoridad `ROLE_<código>` **por cada** elemento de `roles`.
Cuando `roles` no está presente —tokens emitidos antes de la versión 1.1, válidos hasta que
expiren— se usa `role` como respaldo.

`GET /api/v1/me` devuelve los mismos dos campos con idéntico criterio, de modo que el token y el
perfil no pueden contradecirse. Antes `/me` resolvía el rol con `ORDER BY r.id LIMIT 1`, que
devolvía `USER` mientras el token decía `ADMIN`.

Un rol adicional **no** otorga permisos de otro: cada autoridad se concede solo si el rol está en
`user_roles`. Verificado en `CatalogAndAuthorizationTest.aUserWithSeveralRolesGetsAllOfThem` y
`aUserWithASingleRoleDoesNotGainOthers`.

## Recuperación de contraseña — versión 1.2

Alcance: HU-006. **Cambio incompatible:** se retira `POST /api/auth/password-reset`, que cambiaba la
contraseña de cualquier cuenta conociendo únicamente su email, sin ninguna prueba de posesión del
buzón. En su lugar:

* `POST /api/auth/recovery/request` recibe `email`. Devuelve **siempre** `200` con el mismo cuerpo,
  exista o no la cuenta: distinguir los dos casos convertiría el endpoint en un oráculo para enumerar
  los correos registrados. El cuerpo lleva `message`, y lleva además `token` **solo** cuando
  `app.recovery.expose-token` es `true`, que es el canal de desarrollo que admite RF-03 porque el
  envío de correo es opcional. El valor por defecto de esa propiedad es `false`.
* `POST /api/auth/recovery/confirm` recibe `token` y `password`. Devuelve `204`. Un token inexistente,
  vencido o ya usado devuelve `400` sin alterar la contraseña, y lo mismo una contraseña de menos de
  8 caracteres. Consumir el token **revoca todas las sesiones vivas** de la cuenta.

El token es de 32 bytes aleatorios en hexadecimal, vive 30 minutos y es de un solo uso. De él se
almacena únicamente el SHA-256 en `password_reset_tokens`. Pedir un token nuevo invalida el anterior,
de modo que un token filtrado deja de servir en cuanto el titular legítimo vuelve a solicitarlo.

Evidencia cross-repo: `PasswordRecoveryTest` (11 pruebas) en el backend y la pantalla de recuperación
en `FCV_Citas-Web/src/app/pages/recovery.ts`, que ahora exige el código y ya no afirma que la cuenta
exista.
