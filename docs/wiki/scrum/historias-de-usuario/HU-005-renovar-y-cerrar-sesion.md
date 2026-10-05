---
id: HU-005
tipo: historia-de-usuario
titulo: "Renovar y cerrar sesión"
estado: Completada
epica: "[[EP-002-identidad-sesion-y-perfil]]"
esfuerzo: Alto
sprint_sugerido: "Incremento II — Acceso y datos maestros"
dependencias: ["[[HU-004-iniciar-sesion-jwt]]"]
relacionadas: ["[[HU-006-recuperar-contrasena]]"]
---

# HU-005 — Renovar y cerrar sesión

## Historia de usuario
**COMO** usuario autenticado **QUIERO** renovar una sesión válida y cerrarla al terminar **PARA** conservar acceso controlado y revocar el token de refresco.

## Alcance
- Refresh con token válido y logout/revocación conforme al contrato aprobado.

## Fuera de alcance
- Extender indefinidamente sesiones, reutilizar refresh revocado o exponer credenciales.

## Reglas de negocio
- Access/refresh separados; logout revoca; token inválido/revocado no habilita nueva sesión.

## Dependencias y relaciones
- Épica: [[EP-002-identidad-sesion-y-perfil]]
- Dependencias: [[HU-004-iniciar-sesion-jwt]]
- Relacionada: [[HU-006-recuperar-contrasena]]

## Esfuerzo
**Nivel:** Alto. Requiere ciclo de vida seguro y afecta todos los clientes autenticados.

## Tareas de desarrollo
- [ ] **T-01 — Definir validación, rotación o revocación según contrato aprobado.** Dificultad: Alto. No asumir semántica faltante.
- [ ] **T-02 — Implementar puertos/adaptadores de refresh y logout.** Dificultad: Alto. Aislar persistencia de sesión.
- [ ] **T-03 — Ajustar cliente y pruebas de expiración/revocación.** Dificultad: Medio. Evitar fuga de tokens.

## Criterios de aceptación
### CA-01 — Renovación válida
Dado un refresh token vigente y no revocado, cuando se solicita refresh, entonces se obtiene una sesión renovada según contrato.
### CA-02 — Revocación en logout
Dado un usuario autenticado, cuando cierra sesión, entonces su refresh token deja de permitir renovación.
### CA-03 — Rechazo seguro
Dado un refresh ausente, inválido, vencido o revocado, cuando se usa, entonces se niega sin crear sesión ni revelar datos sensibles.

## Definition of Done
- [ ] CA-01 a CA-03 están probados en aplicación/REST y cliente si existe.
- [ ] Persistencia de tokens/revocación, si aplica, tiene migración Flyway y no persiste secretos inseguros.
- [ ] El cierre borra o invalida el estado cliente autorizado por contrato.
- [ ] La trazabilidad Scrum está actualizada.

## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumplido | `loginReturnsAccessAndRefreshAndRefreshRotates`: el refresh rota y el anterior queda inválido | Rotación atómica |
| CA-02 | Cumplido | `AuthControllerTest.logoutRevokesTheRefreshToken` | Prueba añadida al cerrar S6: no existía |
| CA-03 | Cumplido | `invalidCredentialsAndMalformedRefreshAreRejected` y `logoutWithAnUnknownRefreshIsSilent` | 401 al reutilizar, 204 silencioso al cerrar |
| DoD | Cumplido | 4/4 | El refresh se almacena solo como SHA-256 |

## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-04 — Regularizada al cerrar S6: el alcance de S2 estaba implementado desde `f299ec9` sin evidencia registrada.

## Notas y decisiones
- PREGUNTA ABIERTA: rotación y persistencia concreta del refresh token.
