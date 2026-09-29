# Contrato REST — Disponibilidad

**HU:** HU-019. **Versión:** 1.0.

`GET /api/v1/availability?date=YYYY-MM-DD&specialtyId={id}&locationId={id}&professionalId={id}` requiere JWT válido. Devuelve únicamente slots libres de profesionales activos, con la especialidad activa asociada y sede habilitada. Para especialidades de 60 minutos solo devuelve inicios con dos slots consecutivos. Los filtros de sede y profesional son opcionales; la fecha y especialidad son obligatorios.
