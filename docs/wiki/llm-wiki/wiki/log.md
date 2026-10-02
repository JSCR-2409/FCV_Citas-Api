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

# 2026-10-02 — Multi-rol en el token

**DECISIÓN:** El access token pasa a llevar `roles` con todos los códigos del usuario, además del
`role` de presentación que se conserva por compatibilidad. `JwtAuthFilter` concede una autoridad
por cada rol, con respaldo en `role` para los tokens ya emitidos. Motivo: `user_roles` es N:M y el
PRD admite usuarios con varios roles, pero `getPrimaryRole()` colapsaba la lista al primero en
orden alfabético, de modo que un usuario con ADMIN y PROFESSIONAL solo podía actuar como ADMIN.
Contrato `auth-rest.md` elevado a 1.1.

**HECHO:** `GET /api/v1/me` resolvía el rol con `ORDER BY r.id LIMIT 1` y devolvía `USER` mientras
el token decía `ADMIN`. Ahora ambos usan el mismo criterio y `/me` expone también `roles`.

**HECHO:** `JwtService` seguía trayendo los secretos JWT por defecto en el propio código Java
(`getProperty(clave, "development-...-change-me-32")`), así que quitarlos de `application.yml` no
había logrado el fail-fast. Se usa `getRequiredProperty`. Se añadió al escáner de secretos una
regla para este patrón, que antes no detectaba.

**HECHO:** El frontend permite la ruta si el usuario tiene ese rol entre los suyos, y la barra de
navegación muestra solo los portales que le corresponden; antes ofrecía los tres a cualquier
usuario autenticado.

**HECHO:** Eliminado código muerto del prototipo: `loginAs()` con tres perfiles ficticios,
`addAppointment()` y `cancelAppointment()` locales, y el bloque inalcanzable tras un `return` en
`handleBookAppointment`.

# 2026-10-02 — Reprogramación de citas (HU-024, HU-029, HU-030)

**HECHO:** La reprogramación no existía en ninguna capa: sin endpoints, sin UI y sin contrato. La
tabla `reschedule_requests` existía desde `V7` pero nada la leía ni la escribía. Implementadas las
tres HU con 22 pruebas, una por criterio y varias por regla.

**DECISIÓN:** La retención doble de RN-10 se resuelve sin columnas nuevas: mientras la solicitud
está `PENDING`, los slots propuestos se marcan con el mismo `appointment_id` de la cita, así que
esta retiene su franja original y la propuesta, y ningún otro paciente puede tomarlas. Qué franja es
cada una se deduce de `previous_*` y `requested_*` de la solicitud, lo que permite liberar
exactamente el lado que corresponde al decidir. Cuando las dos franjas se solapan, el slot
compartido no se libera.

**HECHO:** Añadida una guarda que rechaza con `409` aprobar una propuesta cuya franja ya no está
retenida por la cita. Lo detectó la verificación en vivo: la fila que el seed trae en
`reschedule_requests` se insertó sin reservar su franja, de modo que aprobarla habría movido la cita
a un slot libre que otro paciente podía reservar.

**HECHO:** Cancelar una cita con reprogramación `PENDING` libera ambas franjas y pasa la solicitud a
`CANCELLED`. Migración `V9` siembra ese estado, que ni `V4` ni `V8` incluían.

**HECHO:** `GET /api/v1/me/appointments` expone `professionalId`, `specialtyId` y `durationMinutes`.
Sin ellos la UI no podía consultar franjas del mismo profesional, que es lo que la regla exige.

**HECHO:** El portal del paciente nunca tuvo listado de citas: la tarjeta mostraba `apps[0]` sobre
una lista ordenada de forma descendente, así que presentaba la cita más lejana como la siguiente, y
el contador de próximas incluía las canceladas. Añadido el listado completo separado en próximas e
historial, con los seis estados del PRD traducidos y 6 pruebas de la clasificación.

**Contrato:** nuevo `docs/contratos/reprogramaciones-rest.md` versión 1.0.

# 2026-10-02 — Cierre funcional de S3: portal profesional y CRUD administrativo

**HECHO:** El portal del profesional no hacia ninguna llamada al backend: mostraba ocho pacientes
inventados en el componente y sus acciones solo movian signals locales. Ahora cubre HU-016, HU-017
y HU-018 contra los endpoints ya probados, y lee las sedes de `/api/v1/catalogs/locations` en lugar
de codificarlas. Es el primer consumo real de un endpoint de catalogo: los seis existian sin usarse.

**HECHO:** Eliminados del portal profesional los modales de historia clinica y evolucion. El PRD
excluye explicitamente historia clinica y diagnosticos del alcance, y no guardaban nada.

**DECISIÓN:** Lo que no existe se declara en lugar de simularse. El portal profesional indica que la
agenda de pacientes es HU-025 y el cierre de atencion HU-026; el portal administrativo indica que
EPS y planes son HU-010 y HU-011 y la auditoria HU-031. Antes esos espacios mostraban datos
fabricados que parecian reales.

**HECHO:** Dos endpoints de lectura que faltaban y sin los cuales la administracion era imposible:
`GET /api/v1/admin/professionals`, con las asignaciones agrupadas e incluyendo los inactivos que hay
que poder reactivar, y `GET /api/v1/admin/specialties`, que a diferencia del catalogo publico
devuelve tambien las desactivadas.

**HECHO:** Corregido un defecto en `PATCH /api/v1/admin/specialties/{id}`: no validaba la duracion,
asi que aceptaba 45 minutos pese a que HU-012 CA-01 dice "crea o actualiza". La prueba anterior solo
cubria el alta. Suite de 83 a 88 pruebas.

**HECHO:** El portal administrativo cubre HU-012 alta, cambio de duracion y desactivacion de
especialidades; HU-013 alta de profesionales; HU-014 asignacion de especialidades forzando una sola
principal en la propia UI; y HU-015 seleccion de sedes y conmutador de estado operativo.

**HECHO:** Backlog: 16 HU `Completada`, ninguna en validacion, 18 pendientes de S4 y S5.
