# Contrato REST — Reprogramación de citas

**HU:** HU-024, HU-029 y HU-030. **Versión:** 1.0.

## Retención doble (RN-10)

Mientras la solicitud está `PENDING`, **la cita retiene las dos franjas**: la original y la
propuesta. Se consigue marcando los slots propuestos con el mismo `appointment_id` de la cita, de
modo que ningún otro paciente puede reservarlos y la cita original no se pierde. Qué franja es cada
una se deduce de `previous_start_at`/`previous_end_at` y `requested_start_at`/`requested_end_at` de
la solicitud, que es lo que permite liberar exactamente el lado que corresponde al decidir.

Cuando las dos franjas se solapan —por ejemplo mover una cita de 60 minutos media hora más tarde—
el slot compartido **no** se libera.

## `POST /api/v1/me/appointments/{id}/reschedule-requests`

Requiere el JWT del paciente dueño de la cita. Recibe `{ slotId }`.

Condiciones, todas verificadas en el backend:

* la cita es propia, está en `APPROVED` y es futura;
* no existe ya una solicitud `PENDING` para esa cita;
* la franja propuesta pertenece **al mismo profesional** y a **la misma especialidad**: cambiar de
  profesional es una cita nueva, no una reprogramación;
* la franja propuesta es futura, distinta de la actual y completa la duración de la especialidad
  (60 minutos exigen dos slots consecutivos);
* el profesional está activo y la especialidad sigue asociada a él.

Responde `201` con `{ id, appointmentId, status: PENDING, previousStartAt, previousEndAt,
requestedStartAt, requestedEndAt }`. Devuelve `400` por datos inválidos y `409` cuando alguna de las
condiciones anteriores no se cumple, **sin retener franja alguna**.

## `GET /api/v1/me/reschedule-requests`

El paciente consulta sus propias solicitudes, para ver el estado y el motivo de un rechazo. Nunca
expone solicitudes de otros pacientes. Cada elemento trae `status`, `previousStartAt`,
`requestedStartAt`, `decisionReason`, `patientAction`, `specialtyName` y `locationName`.

## `PATCH /api/v1/me/reschedule-requests/{id}/action`

Tras un rechazo el paciente decide: `{ action: KEEP_APPOINTMENT | CANCEL_APPOINTMENT }`.
Conservar no altera la cita; cancelar aplica la misma regla que la cancelación normal y libera la
franja. Solo opera sobre una solicitud propia y ya `REJECTED`; en otro caso devuelve `409`.

## `GET /api/v1/admin/reschedule-requests`

Requiere `ADMIN`. Lista únicamente las solicitudes `PENDING`, con los datos que HU-029 CA-02 exige
para comparar antes de decidir:

| Campo | Descripción |
|---|---|
| `id`, `appointmentId` | Solicitud y cita afectada |
| `patientName` | Paciente solicitante |
| `professionalId`, `professionalCode`, `professionalName` | Profesional, que se conserva |
| `specialtyId`, `specialtyName`, `durationMinutes` | Especialidad, que se conserva |
| `locationId`, `locationName` | Sede de la franja propuesta |
| `previousStartAt`, `previousEndAt` | Franja actual de la cita |
| `requestedStartAt`, `requestedEndAt` | Franja propuesta |
| `status` | Siempre `PENDING` |

Filtros opcionales y combinables: `locationId`, `professionalId`, `specialtyId` y `date`
(`YYYY-MM-DD`, sobre la fecha de la franja propuesta).

## `PATCH /api/v1/admin/reschedule-requests/{id}`

Requiere `ADMIN`. Recibe `{ status: APPROVED|REJECTED, reason? }`.

* `APPROVED`: libera la franja original, deja la propuesta asignada y actualiza
  `scheduled_start_at`, `scheduled_end_at` y `location_id` de la cita. La cita permanece `APPROVED`.
* `REJECTED`: exige `reason`, lo persiste en `decision_reason`, libera la propuesta y **mantiene la
  cita en su franja original**.
* `400` por `status` inválido o rechazo sin motivo; `404` si la solicitud no existe.
* `409`, sin cambios parciales, cuando la solicitud ya fue resuelta o cuando la franja propuesta ya
  no está retenida por esa cita. El segundo caso cubre una retención perdida por una liberación
  externa o una solicitud creada sin reservar su franja: aprobarla dejaría la cita sobre slots que
  otro paciente podría reservar.

## Efecto de cancelar la cita

Cancelar una cita con una reprogramación `PENDING` libera ambas franjas y pasa la solicitud a
`CANCELLED`, para que el ADMIN no decida sobre una cita que ya no existe.

## Verificación

`ReschedulingTest` cubre los nueve criterios de las tres HU con 22 pruebas, incluidos el solape de
franjas, la retención frente a otro paciente, la decisión doble y la propuesta sin retener.
