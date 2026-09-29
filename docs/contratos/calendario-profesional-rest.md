# Contrato REST — Calendario profesional

**HU:** HU-017/HU-018. **Versión:** 1.0.

`GET /api/v1/professional/availability-blocks?from=YYYY-MM-DD&to=YYYY-MM-DD` devuelve únicamente bloques propios; el período por defecto es hoy a 30 días.

`PUT /api/v1/professional/availability-blocks/{id}` edita un bloque futuro propio sin slots comprometidos y regenera sus slots de 30 minutos. `DELETE` realiza baja lógica bajo las mismas condiciones. Un bloque ajeno/inexistente responde `404`; un bloque con slot comprometido `409`.
