# Contrato REST — Bloques de disponibilidad

**HU:** HU-016. **Versión:** 1.0.

`POST /api/v1/professional/availability-blocks` requiere un JWT de `PROFESSIONAL`. Recibe `{ locationId, availableDate, startTime, endTime }`, exige fecha futura, límites alineados a 30 minutos, sede asignada, profesional activo y ausencia de solapamiento. Crea el bloque y sus slots de 30 minutos en una transacción; responde `201` con el identificador y los datos del bloque. Devuelve `400` por datos inválidos, `403` por sede no asignada/profesional inactivo y `409` por solapamiento.
