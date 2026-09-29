# citas-api

Repositorio backend del proyecto. **No contiene implementación de negocio inicial**.

## Debe ser construido por el estudiante
- Java 21 + Spring Boot 3.5.x + Maven.
- Arquitectura hexagonal.
- MySQL + Flyway.
- Spring Security + JWT access/refresh.
- REST.
- Pruebas.

## Documentación compartida
- `docs/wiki/scrum/`: épicas/HU generadas con la Skill Scrum.
- `docs/wiki/llm-wiki/`: única LLM Wiki global del workspace.
- `automations/n8n/`: JSON exportados en S5/S6.

Lee el PRD en la carpeta raíz antes de inicializar Spring Boot.

## Desarrollo con Docker

Desde la raíz del workspace, `docker compose up -d` arranca MySQL, esta API y Angular. La salud de la API se expone en `http://localhost:8080/actuator/health`; el origen CORS de desarrollo es `http://localhost:4200`, configurable mediante `FRONTEND_ORIGIN`.
