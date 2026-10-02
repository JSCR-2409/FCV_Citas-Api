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
