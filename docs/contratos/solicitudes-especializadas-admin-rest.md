# Contrato REST — Solicitudes especializadas ADMIN

**HU:** HU-027/HU-028. **Versión:** 1.1.

Ambos endpoints requieren `ADMIN`.

## `GET /api/v1/admin/specialized-requests`

Lista únicamente las citas en estado `REQUESTED`. Cada elemento entrega los datos que HU-021 CA-03
exige para decidir sin consultas adicionales:

| Campo | Descripción |
|---|---|
| `id` | Identificador de la cita |
| `patientUserId`, `patientName` | Paciente solicitante |
| `professionalId`, `professionalCode` | Profesional asignado |
| `locationId`, `locationName` | Sede |
| `specialtyId`, `specialtyName` | Especialidad |
| `durationMinutes` | Duración que fija la especialidad (30 o 60) |
| `startAt`, `endAt` | Franja reservada, en hora local sin desplazamiento |
| `status` | Siempre `REQUESTED` |

Filtros opcionales y combinables, conforme a HU-027 CA-02: `locationId`, `professionalId`,
`specialtyId` y `date` (`YYYY-MM-DD`, compara la fecha de `scheduled_start_at`). Sin filtros
devuelve todas las solicitudes pendientes. Un filtro sin coincidencias devuelve `[]`.

## `PATCH /api/v1/admin/specialized-requests/{id}`

Recibe `{ status: APPROVED|REJECTED, reason? }`.

* `APPROVED` deja la cita en `APPROVED` y **conserva** los slots retenidos.
* `REJECTED` exige `reason` no vacío, lo **persiste** en `appointments.reason` y libera los slots.
* Respuestas de error: `400` por `status` inválido o rechazo sin motivo; `404` si la cita no existe;
  `409` si la cita ya no está en `REQUESTED` —porque ya fue resuelta o porque nunca pasó por esta
  bandeja, como una cita general—, sin transición ni liberación de franjas.

## Cambios respecto a la versión 1.0

La 1.0 describía la lista como «citas `REQUESTED` con paciente, profesional, sede, especialidad y
franja» sin fijar los nombres de campo ni los filtros, y no especificaba el código de error para una
solicitud ya resuelta. La 1.1 nombra los campos, añade `durationMinutes`, documenta los cuatro
filtros de HU-027 CA-02 y fija `409` para la decisión sobre una cita que ya no está pendiente.
