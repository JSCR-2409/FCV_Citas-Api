# Convenciones de la LLM Wiki

- `raw/`: fuentes aprobadas, curadas e inmutables.
- `wiki/`: síntesis enlazadas y mantenidas por el agente.
- `schema/`: estas convenciones y workflows.
- `wiki/index.md` se lee primero y se actualiza cuando cambia el catálogo.
- `wiki/log.md` es append-only.

Cada afirmación durable debe clasificarse como `HECHO`, `DECISIÓN`, `PREFERENCIA` o `PREGUNTA ABIERTA`, e indicar su fuente cuando sea posible. Nunca guardar secretos, tokens, contraseñas ni PII innecesaria.
