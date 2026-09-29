# Contrato REST — Catálogos fijos

**HU:** HU-009. **Versión:** 1.0.

Los recursos son de solo lectura y requieren un access JWT válido. No se aceptan operaciones de escritura en estas rutas.

| Método y ruta | Respuesta `200` |
| --- | --- |
| `GET /api/v1/catalogs/roles` | arreglo de `{ id, code, name }` |
| `GET /api/v1/catalogs/appointment-statuses` | arreglo de `{ id, code, name }` |
| `GET /api/v1/catalogs/reschedule-statuses` | arreglo de `{ id, code, name }` |
| `GET /api/v1/catalogs/regimes` | arreglo de `{ id, code, name }` |
| `GET /api/v1/catalogs/locations` | arreglo de `{ id, code, name, address }` |

Sin autenticación, la respuesta es `403` según la configuración actual de Spring Security. Los `code` son identificadores estables para el contrato; los nombres son etiquetas de presentación. Las sedes sembradas son HIC e ICV, conforme al PRD.
