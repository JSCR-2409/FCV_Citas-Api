# Contrato REST — Solicitudes especializadas ADMIN

**HU:** HU-027/HU-028. **Versión:** 1.0.

`GET /api/v1/admin/specialized-requests` lista citas `REQUESTED` con paciente, profesional, sede, especialidad y franja. `PATCH /api/v1/admin/specialized-requests/{id}` recibe `{ status: APPROVED|REJECTED, reason? }`; el rechazo exige motivo y libera los slots retenidos. Ambos endpoints requieren `ADMIN`.
