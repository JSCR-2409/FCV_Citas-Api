# Contrato REST de agenda profesional, auditoría e integraciones

Alcance: HU-022, HU-025, HU-026, HU-031, HU-032, HU-033 y HU-034. Versión 1.0.

## Mis citas, con filtros — HU-022

* `GET /api/v1/me/appointments` admite `status`, `from` y `to`. Devuelve `200` con el arreglo de citas
  del usuario del token.

Cada elemento lleva `id`, `startAt`, `endAt`, `status`, `doctorName`, `specialty`, `facility`,
`facilityFullName`, `professionalId`, `specialtyId`, `durationMinutes` y `reason`.

* `reason` es el motivo del rechazo administrativo. Viaja **siempre**, con `null` cuando no aplica, en
  lugar de omitirse: así el cliente no tiene que distinguir ausencia de campo y ausencia de valor.
* `status` admite varios códigos separados por coma, de modo que la vista de citas activas se resuelve
  en una sola llamada.
* **DECISIÓN — el filtro de fecha es inclusivo en ambos extremos.** `from` y `to` son fechas, no
  instantes: el rango se traduce a `[from 00:00, to+1día 00:00)`. Tratar `to` como medianoche
  excluiría casi todas las citas de ese día.
* **DECISIÓN — orden descendente y sin paginación.** Un paciente tiene decenas de citas, no miles. La
  UI separa «Próximas» e «Historial» a partir de esa lista.

## Agenda del profesional — HU-025

* `GET /api/v1/professional/appointments` admite `from`, `to`, `locationId` y `status`. Devuelve `200`
  con `from`, `to`, `count` e `items`, o `403` si el usuario del token no tiene una ficha de
  profesional activa.

**No hay parámetro de profesional.** RF-16 limita lo que el profesional puede ver a «sus propias
citas», y un identificador en la petición sería exactamente el agujero que la regla prohíbe: se deriva
del token.

El rango por defecto es el día de hoy, que es la vista que necesita quien va a atender. Sin `status`
devuelve `APPROVED`, `COMPLETED` y `NO_SHOW`; las demás siguen siendo consultables pasándolo.

Cada elemento lleva `patientName` y `patientDocument`, pero **no el correo ni el teléfono**: el
documento identifica al paciente en la consulta sin convertir la agenda en un directorio de contacto.

## Cierre de atención — HU-026

* `PATCH /api/v1/professional/appointments/{id}/attention` recibe `outcome`, que debe ser `COMPLETED`
  o `NO_SHOW`, y opcionalmente `notes`. Devuelve `200`, `400` si el resultado es otro, `404` si la cita
  no es de ese profesional, o `409` si no es cerrable.

Una cita es cerrable si es de ese profesional, está `APPROVED` y su hora de inicio ya pasó. Las tres
condiciones van en la guarda del mismo `UPDATE`, no comprobadas por separado: así no hay ventana entre
la lectura y la escritura.

`404` y no `403` cuando la cita es de otro profesional: responder distinto permitiría descubrir qué
citas existen en la agenda de otro.

**La franja no se libera.** La atención ocurrió, y liberarla permitiría reservar sobre un horario ya
consumido.

## Auditoría de cambios de estado — HU-031

* `GET /api/v1/admin/appointment-history` admite `appointmentId`, `from`, `to` y `limit`. Devuelve
  `200` con `count` e `items`.

Cada entrada lleva `id`, `appointmentId`, `statusCode`, `changeSource`, `reason`, `changedAt`,
`actorId` y `actorName`. `actorId` y `actorName` son `null` cuando `changeSource` es `SYSTEM`: una
transición del sistema no tiene responsable humano, y el contrato lo dice con `null` en lugar de
inventar un usuario.

Con `appointmentId` el orden es **cronológico ascendente**, porque lo que interesa es la secuencia; sin
él es descendente, porque lo relevante es lo último que pasó. `limit` se acota a 500.

Vive bajo `/admin` porque expone quién decidió sobre una cita: no es información del paciente ni del
profesional.

### Transiciones que se auditan

| Momento | `changeSource` | Actor |
|---|---|---|
| Reserva general o solicitud especializada | `USER` | el paciente |
| Decisión administrativa de una especializada | `ADMIN` | el ADMIN |
| Aprobación de una reprogramación | `ADMIN` | el ADMIN |
| Cancelación por el paciente | `USER` | el paciente |
| Cierre de atención | `USER` | el profesional |

El estado inicial también se registra: sin él la auditoría empezaría a contar la vida de la cita por
su segundo estado.

## Integraciones n8n — HU-032, HU-033 y HU-034

### Autenticación

Los endpoints bajo `/api/v1/integrations/**` exigen la cabecera `X-Integration-Token` con el valor de
`INTEGRATION_TOKEN`. Cualquier otra cosa recibe `403`.

**DECISIÓN — token de servicio y no un JWT de usuario.** n8n no es una persona: no tiene perfil, no
renueva sesión y no debe quedar atado a la cuenta de nadie, porque desactivar a ese usuario rompería
la automatización en silencio.

El token concede una autoridad propia, `ROLE_INTEGRATION`, que **no reutiliza `ADMIN`**: no abre los
endpoints administrativos ni los del paciente. Verificado en prueba. La comparación es en tiempo
constante, y si la variable no está definida los endpoints quedan cerrados.

### Citas próximas — HU-032

* `GET /api/v1/integrations/appointments/upcoming` admite `withinHours`. Devuelve `200` con
  `generatedAt`, `windowHours`, `windowEnd`, `count` e `items`.

**DECISIÓN — criterio de «próxima».** Ventana paramétrica con 24 horas por defecto, acotada a 14 días.
El criterio vive en el contrato y no en el workflow, de modo que cambiarlo no exige reeditar el JSON.

Solo devuelve citas `APPROVED`. Una `CANCELLED` o `REJECTED` no se selecciona porque el filtro es por
código de estado, no por ausencia de cancelación.

Cada elemento lleva `appointmentId`, `startAt`, `endAt`, `patientName`, `patientEmail`,
`professionalName`, `specialtyName`, `durationMinutes`, `locationName` y `locationAddress`. **No lleva
el documento ni el teléfono:** el recordatorio se redacta y se envía con el correo.

### Resumen operativo diario — HU-034

* `GET /api/v1/integrations/daily-summary` admite `date`. Devuelve `200` con `date`, `total`,
  `byLocationAndStatus` y `bySpecialty`.

Devuelve **conteos, no citas**: un resumen no necesita datos de ningún paciente.

### Webhook de salida — HU-033

Cuando `STATUS_WEBHOOK_URL` está configurada, el backend hace `POST` a esa URL en tres momentos:

| Evento | Cuándo |
|---|---|
| `APPOINTMENT_DECIDED` | el ADMIN aprueba o rechaza una cita especializada |
| `RESCHEDULE_DECIDED` | el ADMIN aprueba o rechaza una reprogramación |
| `APPOINTMENT_CANCELLED` | el paciente cancela su cita |

El payload lleva `event`, `appointmentId`, `status`, `startAt`, `patientName`, `patientEmail`,
`professionalName`, `specialtyName`, `locationName` y `reason`.

**Autenticación: JWT de dos minutos.** La petición lleva `Authorization: Bearer <jwt>`, HS256 firmado
con `STATUS_WEBHOOK_SECRET`, con `iss=citas-api`, `sub=status-webhook` y `exp` a 120 segundos. n8n lo
verifica de forma nativa con la autenticación JWT del nodo Webhook, de modo que el secreto vive en una
credencial de n8n y no en el JSON versionado.

Antes se enviaba un HMAC-SHA256 del cuerpo en `X-Signature` y se retiró, porque **nadie lo
verificaba**: comprobarlo en n8n exigiría que un nodo Code tuviera el secreto, y eso lo dejaría dentro
del JSON. Una firma que nadie verifica no es una defensa.

**El secreto debe tener al menos 32 caracteres.** Si la URL está configurada y el secreto es más
corto, el notificador queda **desactivado** y lo registra con un `ERROR` al arrancar. Enviar el evento
sin firmar sería peor que no enviarlo.

Tres propiedades del envío, todas por la misma razón —una automatización de notificación no puede
influir en la operación clínica—:

1. **Asíncrono.** Un n8n lento no alarga la respuesta que espera el usuario.
2. **Sin propagar errores.** Que n8n esté caído no puede impedir que un ADMIN apruebe una cita.
3. **Desactivado por defecto.** Sin la URL no se intenta ningún envío, de modo que el entorno de
   desarrollo y las pruebas no dependen de una instancia externa.

La URL del webhook **nunca se registra en el log**: puede llevar un identificador secreto en la ruta.

## Límite de peticiones

Dos rutas tienen cupo por IP, con `429` y `Retry-After` cuando se agota:

| Ruta | Cupo por minuto | Propiedad |
|---|---|---|
| `/api/auth/recovery/**` | 5 | `app.rate-limit.recovery-per-minute` |
| `/api/v1/integrations/**` | 60 | `app.rate-limit.integration-per-minute` |

Son las dos abusables sin estar autenticado o con una sola credencial: la recuperación permite generar
tokens en cantidad, y pedir uno nuevo invalida el anterior, de modo que sin límite cualquiera podría
invalidar de forma repetida el token legítimo de otra persona; la integración permite probar el token
de servicio en bucle.

El filtro se ejecuta **antes de Spring Security**, para que el `403` de un token inválido no se adelante
al contador. El resto de la API no está limitada.

## Evidencia cross-repo

Backend: `ProfileAndMyAppointmentsTest` (5 de HU-022), `ProfessionalAgendaAndAuditTest` (19) e
`IntegrationEndpointsTest` (13). Frontend: agenda y cierre en `doctor-portal.ts`, panel de auditoría en
`admin-portal.ts` y motivo de rechazo en `patient-portal.ts`, con una prueba en
`clinical-data.spec.ts`. Los tres workflows, en `automations/n8n/`.
