# Contrato REST — Especialidades

**HU:** HU-012. **Versión:** 1.0.

Las especialidades tienen nombre único, duración de `30` o `60` minutos y activación lógica. Los endpoints `/api/v1/admin/**` requieren el rol `ADMIN`; la lectura del catálogo activo requiere un access JWT válido.

| Método y ruta | Solicitud | Respuesta |
| --- | --- | --- |
| `GET /api/v1/catalogs/specialties` | — | `200` con especialidades activas `{ id, name, durationMinutes }` |
| `GET /api/v1/admin/specialties` | — | `200` con todas `{ id, name, durationMinutes, active }` |
| `POST /api/v1/admin/specialties` | `{ name, durationMinutes }` | `201` con la especialidad creada |
| `PATCH /api/v1/admin/specialties/{id}` | cualquiera de `name`, `durationMinutes`, `active` | `200` con la especialidad actualizada |

`durationMinutes` solo acepta `30` o `60`. Un nombre duplicado devuelve `409`; una duración inválida, `400`; un id inexistente, `404`; y acceso no autorizado, `403`.
