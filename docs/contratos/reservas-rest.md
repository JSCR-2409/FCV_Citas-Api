# Contrato REST — Reservas

**HU:** HU-020/HU-021. **Versión:** 1.0.

- `POST /api/v1/appointments/general` crea una cita `APPROVED` para una especialidad general elegible.
- `POST /api/v1/appointments/specialized` crea una cita `REQUESTED` y retiene sus slots para decisión administrativa.

Ambos reciben `{ slotId, specialtyId }`, requieren USER autenticado y responden `201` con la cita. Una franja ocupada, retenida o insuficiente para 60 minutos responde `409`; una especialidad inactiva/no asociada responde `400`.
