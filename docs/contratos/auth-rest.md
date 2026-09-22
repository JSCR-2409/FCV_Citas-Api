# Contrato REST de identidad y sesión

Alcance: HU-003, HU-004 y HU-005. La API es REST directo; no expone contraseñas, hashes ni tokens en logs.

* `POST /api/auth/register` recibe `names`, `surnames`, `documentType`, `documentNumber`, `email`, `phone` y `password`. Devuelve `201` sin cuerpo. Email y pareja tipo/número de documento son únicos; conflicto devuelve `409`.
* `POST /api/auth/login` recibe `email` y `password`. Devuelve `200` con `accessToken`, `refreshToken`, `tokenType=Bearer`, `expiresIn`; credenciales inválidas devuelven `401` sin detalle sensible.
* `POST /api/auth/refresh` recibe `refreshToken`. Devuelve una nueva pareja de tokens y rota atómicamente el refresh anterior. Un refresh inválido, vencido o reutilizado devuelve `401`.
* `POST /api/auth/logout` recibe `refreshToken`, lo revoca y devuelve `204`.

El access token es JWT de corta duración (15 minutos por configuración); el refresh es JWT separado (7 días por configuración), almacenado únicamente como SHA-256 en `refresh_sessions`. Los roles viajan como claim `role`. Secretos y duración se configuran por variables de entorno.
