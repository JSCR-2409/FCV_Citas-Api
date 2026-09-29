# Registro de Wiki

Registro cronológico append-only. Formato: `fecha | operación | fuentes | resultado`.

2026-09-17 | INIT | PRD.md, RESTRICCIONES_TECNICAS.md, database/REQUISITOS_NORMALIZACION_3FN.md, README.md, citas-api/README.md, citas-web/README.md | Se creó la estructura inicial y el índice; no se persistieron decisiones de implementación.
# 2026-09-24 — Desarrollo Docker

**DECISIÓN:** Se documentó el arranque integrado de MySQL, API y Angular mediante Docker Compose, con healthchecks y CORS local para `http://localhost:4200`.
