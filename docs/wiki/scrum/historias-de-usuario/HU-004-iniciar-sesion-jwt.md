---
id: HU-004
tipo: historia-de-usuario
titulo: "Iniciar sesión con JWT"
estado: Pendiente de aprobación
epica: "[[EP-002-identidad-sesion-y-perfil]]"
esfuerzo: Alto
sprint_sugerido: "Incremento II — Acceso y datos maestros"
dependencias: ["[[HU-003-registrar-usuario]]"]
relacionadas: ["[[HU-005-renovar-y-cerrar-sesion]]"]
---

# HU-004 — Iniciar sesión con JWT

## Historia de usuario
**COMO** USER, PROFESSIONAL o ADMIN **QUIERO** iniciar sesión con email y contraseña **PARA** acceder solo a mis capacidades autorizadas.

## Alcance
- Autenticar credenciales y emitir access token de corta duración y refresh token separados con contexto de rol.

## Fuera de alcance
- SSO, autenticación social, exponer tokens en logs o autorizar por UI sin control backend.

## Reglas de negocio
- JWT access/refresh separados; roles en contexto de autorización; secretos por environment; credenciales inválidas no revelan información sensible.

## Dependencias y relaciones
- Épica: [[EP-002-identidad-sesion-y-perfil]]
- Dependencias: [[HU-003-registrar-usuario]]
- Relacionada: [[HU-005-renovar-y-cerrar-sesion]]

## Esfuerzo
**Nivel:** Alto. Afecta seguridad, sesión, contrato y todas las rutas autorizadas.

## Tareas de desarrollo
- [ ] **T-01 — Definir caso de uso de autenticación y autorización por rol.** Dificultad: Alto. Mantener secreto fuera del repositorio.
- [ ] **T-02 — Emitir access/refresh y registrar estado de sesión requerido.** Dificultad: Alto. Alinear con HU-005.
- [ ] **T-03 — Integrar manejo de sesión cliente y rutas visibles.** Dificultad: Medio. Sin exponer tokens en UI/logs.

## Criterios de aceptación
### CA-01 — Credenciales válidas
Dado un usuario activo con credenciales correctas, cuando inicia sesión, entonces recibe los tokens separados definidos por contrato y el rol aplicable.
### CA-02 — Credenciales inválidas
Dado email o contraseña incorrectos, cuando inicia sesión, entonces se niega acceso sin divulgar contraseña, token ni detalle sensible.
### CA-03 — Autorización efectiva
Dado un token con rol, cuando intenta una capacidad protegida, entonces backend permite o niega según dicho rol y ownership aplicable.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de seguridad/REST relevantes y evidencia de integración frontend cuando exista.
- [ ] Configuración JWT, CORS y secretos usan environment sin valores reales versionados.
- [ ] Access y refresh no se intercambian ni aparecen en logs, errores o documentación pública.
- [ ] La trazabilidad Scrum está actualizada.

## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Pendiente | — | — |
| CA-02 | Pendiente | — | — |
| CA-03 | Pendiente | — | — |
| DoD | Pendiente | — | — |

## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.

## Notas y decisiones
- PREGUNTA ABIERTA: duración exacta y transporte de cada token deben aprobarse en HU-002.
