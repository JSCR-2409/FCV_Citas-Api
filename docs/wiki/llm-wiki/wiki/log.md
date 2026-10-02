# Registro de Wiki

Registro cronológico append-only. Formato: `fecha | operación | fuentes | resultado`.

2026-09-17 | INIT | PRD.md, RESTRICCIONES_TECNICAS.md, database/REQUISITOS_NORMALIZACION_3FN.md, README.md, citas-api/README.md, citas-web/README.md | Se creó la estructura inicial y el índice; no se persistieron decisiones de implementación.
# 2026-09-24 — Desarrollo Docker

**DECISIÓN:** Se documentó el arranque integrado de MySQL, API y Angular mediante Docker Compose, con healthchecks y CORS local para `http://localhost:4200`.

# 2026-10-02 — Correcciones S3 y red de pruebas

**HECHO:** Se añadieron las migraciones `V7` (appointments, appointment_status_history,
reschedule_requests, eps, eps_plans, user_insurance_affiliations, password_reset_tokens) y `V8`
(estados REJECTED, CANCELLED, COMPLETED, NO_SHOW y estados de reprogramación). Idempotentes: en la
BD inicializada desde `db.sql` son no-ops.

**HECHO:** Corregidos cinco defectos verificados contra MySQL y cubiertos por pruebas:
desplazamiento horario de +5 h en `/availability`, `/me/appointments` y la bandeja ADMIN;
regla de 60 minutos que ofrecía una franja solapada y ocultaba disponibilidad real; ausencia de
guarda de estado en `PATCH /admin/specialized-requests/{id}`, que permitía redecidir cualquier cita
y liberar su franja; motivo de rechazo no persistido; y `500` en lugar de `404` ante especialidad
inactiva.

**DECISIÓN:** Las lecturas de `DATETIME` usan `getObject(..., LocalDateTime.class)` en lugar de
`getTimestamp().toLocalDateTime()`, y el contenedor de la API fija `TZ: America/Bogota`.

**DECISIÓN:** Se eliminó el `UPDATE ... JOIN` del reclamo de franjas, sintaxis propia de MySQL, por
una subconsulta portable que conserva la atomicidad y la guarda `appointment_id IS NULL`.

**HECHO:** Contrato `solicitudes-especializadas-admin-rest.md` elevado a 1.1: nombres de campo,
`durationMinutes`, cuatro filtros de HU-027 CA-02 y `409` para solicitud ya resuelta.

**HECHO:** Suite backend de 4 a 37 pruebas. Frontend: `approveItem()` vacío sustituido por el panel
real de aprobación y rechazo; `requireRole` ahora compara el rol, que antes no validaba.

**PREGUNTA ABIERTA:** Dos divergencias contrato/código registradas en
`risks-and-open-questions.md` (401 frente a 403 sin autenticación; 400 frente a 403 por sede no
asignada). No resueltas por inferencia.
