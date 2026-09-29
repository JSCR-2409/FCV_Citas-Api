# Contrato REST — Profesionales

**HU:** HU-013. **Versión:** 1.0.

`POST /api/v1/admin/professionals` requiere rol `ADMIN`. Recibe datos sintéticos del profesional, `temporaryPassword`, `professionalCode` y `licenseNumber`; crea el usuario con rol `PROFESSIONAL` y su ficha profesional en una transacción.

La respuesta `201` es `{ id, professionalCode, licenseNumber, active, email }`. Nunca devuelve ni registra la contraseña temporal o su hash. Una colisión de email, documento, código profesional o matrícula devuelve `409`; una solicitud inválida, `400`; y una llamada sin ADMIN, `403`.
