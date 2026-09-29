# Arquitectura

## Hechos iniciales

- `citas-api` es el backend Spring Boot previsto.
- `citas-web` es el frontend TypeScript previsto.
- La comunicación es REST directo; no existe Express/BFF.
- La arquitectura backend objetivo es hexagonal.
# Desarrollo local con Docker

**DECISIÓN:** `docker compose up -d` inicia MySQL, `citas-api-dev` y `citas-web-dev`. Angular publica el puerto 4200 y la API el 8080. El servicio frontend depende de que la API esté saludable; la API depende de MySQL saludable. `web_node_modules` aísla dependencias Linux del bind mount Windows.
