# Contrato REST — Sedes y estado del profesional

**HU:** HU-015. **Versión:** 1.0.

Todos los endpoints requieren `ADMIN`.

- `PUT /api/v1/admin/professionals/{id}/locations` recibe `{ "ids": [1, 2] }`, reemplaza atómicamente las sedes asignadas y responde `200` con `{ professionalId, locationIds }`. Se exige al menos una sede, sin duplicados y cada id debe existir en el catálogo fijo.
- `PATCH /api/v1/admin/professionals/{id}/active` recibe `{ "active": true|false }` y responde `200` con `{ id, active }`. Un profesional inexistente responde `404`.

No se crean sedes desde este contrato. La aplicación de sede no asignada o profesional inactivo a bloques y disponibilidad queda a cargo de HU-016/HU-019.
