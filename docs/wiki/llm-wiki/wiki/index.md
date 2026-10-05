# LLM Wiki — Índice

Wiki global del workspace `FCV_Proyecto_Citas_v1`. Las fuentes originales curadas e inmutables viven en [`../raw/`](../raw/); las convenciones y workflows viven en [`../schema/`](../schema/).

## Páginas

- [Dominio](domain.md) — actores, capacidades, sedes y reglas de negocio.
- [Arquitectura](architecture.md) — límites entre `citas-api` y `citas-web`.
- [Modelo de datos](data-model.md) — requisitos 3FN y decisiones pendientes.
- [Contratos REST](rest-contracts.md) — contrato frontend/backend aprobado.
- [Integración frontend](frontend-integration.md) — Stitch, AI Studio y REST directo.
- [Decisiones](decisions.md) — decisiones aprobadas y su evidencia.
- [Ramas y entrega](delivery-and-branches.md) — `main`, `develop` y trazabilidad.
- [Automatizaciones](automations.md) — workflows n8n versionados.
- [Preferencias](preferences.md) — convenciones de trabajo durables.
- [Riesgos y preguntas abiertas](risks-and-open-questions.md) — asuntos no resueltos.
- [Registro](log.md) — historial append-only de operaciones de Wiki.

## Evidencia de sesión

Vive fuera de la Wiki, en [`../../../evidencia/`](../../../evidencia/), porque es registro de lo
ocurrido y no síntesis mantenida:

- [`loops/`](../../../evidencia/loops/) — los tres ciclos Builder/Verifier de S4, con log por iteración.
- [`mcp-n8n.md`](../../../evidencia/mcp-n8n.md) — cliente y servidor MCP, workflows creados y ejecuciones controladas.
- [`seguridad-contenido-no-confiable.md`](../../../evidencia/seguridad-contenido-no-confiable.md) — las cuatro superficies de contenido no confiable y los ocho riesgos residuales.

## Estado

Actualizado al 2026-10-04, tras S4, S5 y S6. `automations.md` y
`risks-and-open-questions.md` están mantenidas; el resto conserva el nivel de detalle con que se
crearon y se completa mediante INGEST de fuentes aprobadas, sin inventar decisiones ni contratos.
