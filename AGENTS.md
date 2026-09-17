# citas-api — Instrucciones del agente backend

## Alcance del repositorio

Este repositorio implementa exclusivamente el backend del laboratorio: Java 21, Spring Boot 3.5.x, Maven, REST/JSON, Spring Security con JWT access/refresh, Spring Data JPA, MySQL 8.4 y Flyway.

No modificar `citas-web/`, no crear Express/BFF y no acoplar el backend a React o Angular. El frontend consumirá esta API directamente por REST.

El checkout inicial no define todavía paquetes, módulos Maven, código Java, migraciones ni pruebas. No inventar una estructura de paquetes sin volver a inspeccionar el proyecto una vez inicializado.

## Antes de editar

1. Leer el `AGENTS.md` de la raíz del workspace, el PRD y las restricciones técnicas.
2. Localizar la HU aprobada y su DoD en `docs/wiki/scrum/`.
3. Identificar reglas de negocio, permisos, datos, migraciones y contrato REST afectados.
4. Proponer un plan que enumere archivos, pruebas y, si aplica, impacto para el frontend.

Si no existe una HU o DoD aplicable, como ocurre en el estado inicial del repositorio, no inventar alcance: pedir o registrar la falta de especificación antes de implementar una funcionalidad.

## Arquitectura hexagonal

- El dominio no depende de Spring, JPA, HTTP ni de adaptadores.
- Los casos de uso y la orquestación de reglas viven en aplicación.
- Los puertos expresan dependencias de entrada y salida.
- REST, seguridad, persistencia JPA y clientes externos son adaptadores.
- Los controladores traducen HTTP, validan la entrada de borde y delegan en casos de uso; no concentran reglas de negocio.
- Las transiciones de estado, reservas de slots, reprogramaciones y auditoría deben ser explícitas y verificables.

## Datos y persistencia

- Usar MySQL 8.4, Spring Data JPA y Flyway cuando se implemente persistencia.
- Todo cambio de esquema requiere una migración Flyway versionada, justificación de claves/cardinalidades/dependencias y pruebas relevantes.
- Mantener como mínimo 3FN; usar relaciones para N:M y no duplicar datos de catálogos sin una decisión explícita de snapshot.
- Los catálogos fijos se cargan por seed. Los catálogos configurables referenciados por transacciones no se eliminan físicamente.
- Proteger contra doble reserva y respetar slots consecutivos para citas de 60 minutos.

## Seguridad y datos de laboratorio

- Usar solo variables de entorno para credenciales y secretos. `.env.example` contiene únicamente valores de ejemplo.
- No abrir, registrar, imprimir ni versionar `.env`, tokens, contraseñas, secretos JWT u OAuth.
- Separar access y refresh tokens; aplicar autorización por rol y ownership.
- Usar hash adaptativo de contraseña compatible con Spring Security, validación server-side y CORS explícito.
- No registrar passwords ni tokens, incluso en errores o pruebas.
- Usar únicamente datos sintéticos del laboratorio; no incorporar datos privados reales de FCV.

## Contratos y pruebas

- La API es REST/JSON y su contrato debe documentarse antes o junto con su implementación; no inferir endpoints o DTOs sin fuente aprobada.
- Si cambia un contrato REST, comunicar el impacto al orquestador y exigir evidencia de compatibilidad en `citas-api` y `citas-web`.
- Ejecutar pruebas de dominio, aplicación e integración REST/persistencia que correspondan a la HU.
- Antes de cerrar una tarea, verificar reglas del PRD, arquitectura, migraciones, controles de acceso y DoD; indicar explícitamente lo no verificado.

## Git, documentación y automatizaciones

- `main` es estable y `develop` es la rama de trabajo. Si `develop` no existe, no trabajar en `main`; solicitar o recibir autorización para preparar la línea de trabajo.
- Preservar cambios ajenos y no reescribir historial.
- No mantener una LLM Wiki propia: la Wiki bajo `docs/wiki/llm-wiki/` es global y la mantiene el orquestador.
- Los artefactos Scrum solo se generan bajo `docs/wiki/scrum/` mediante el flujo autorizado; no sustituyen implementación.
- Los workflows n8n se versionan exclusivamente como JSON bajo `automations/n8n/` y nunca incluyen credenciales.
