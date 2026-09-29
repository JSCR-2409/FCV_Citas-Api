# Contrato REST — Asignación de especialidades

**HU:** HU-014. **Versión:** 1.0.

`PUT /api/v1/admin/professionals/{id}/specialties` requiere `ADMIN` y reemplaza de forma atómica las especialidades del profesional.

Recibe `{ "assignments": [{ "specialtyId": 1, "primary": true }] }`. Debe tener al menos una especialidad activa, sin ids repetidos y exactamente una primaria. Devuelve `200` con `{ specialtyId, name, primary }`. Un profesional inexistente devuelve `404`; asignaciones inválidas o especialidades inactivas/inexistentes devuelven `400`.
