---
id: HU-006
tipo: historia-de-usuario
titulo: "Recuperar contraseña"
estado: Pendiente de aprobación
epica: "[[EP-002-identidad-sesion-y-perfil]]"
esfuerzo: Alto
sprint_sugerido: "Incremento II — Acceso y datos maestros"
dependencias: ["[[HU-003-registrar-usuario]]", "[[HU-005-renovar-y-cerrar-sesion]]"]
relacionadas: []
---

# HU-006 — Recuperar contraseña

## Historia de usuario
**COMO** USER que perdió su contraseña **QUIERO** solicitar y completar su restablecimiento **PARA** recuperar acceso de forma segura.

## Alcance
- Solicitud por email; token temporal de un solo uso; cambio de contraseña que consume/invalida token y sesión correspondiente según contrato.

## Fuera de alcance
- SMTP obligatorio, exponer token en producción o recuperar contraseña anterior.

## Reglas de negocio
- Token temporal y de un solo uso; en desarrollo puede existir entrega controlada; nueva contraseña nunca en texto plano.

## Dependencias y relaciones
- Épica: [[EP-002-identidad-sesion-y-perfil]]
- Dependencias: [[HU-003-registrar-usuario]], [[HU-005-renovar-y-cerrar-sesion]]

## Esfuerzo
**Nivel:** Alto. Involucra secretos de corta vida, entrega segura y revocación.

## Tareas de desarrollo
- [ ] **T-01 — Definir caso de solicitud, token temporal y consumo.** Dificultad: Alto. Documentar canal de desarrollo aprobado.
- [ ] **T-02 — Actualizar contraseña con hash y revocaciones aplicables.** Dificultad: Alto. Proteger contra reutilización.
- [ ] **T-03 — Integrar vistas/formularios y errores seguros.** Dificultad: Medio. No mostrar token en UI o logs no autorizados.

## Criterios de aceptación
### CA-01 — Solicitud controlada
Dado un email registrado, cuando se solicita recuperación, entonces se genera un token temporal y el canal de desarrollo aprobado permite completar el ejercicio sin exponerlo indebidamente.
### CA-02 — Cambio de un solo uso
Dado un token válido, cuando se define nueva contraseña, entonces la contraseña cambia, el token queda consumido y no puede reutilizarse.
### CA-03 — Token no válido
Dado token vencido, usado o inválido, cuando se intenta cambiar contraseña, entonces se rechaza sin alterarla.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de token, expiración y consumo; el canal controlado está documentado.
- [ ] Las contraseñas y tokens no se escriben en texto plano, logs ni respuestas no autorizadas.
- [ ] Migración Flyway y revocación de sesión, si el diseño aprobado las requiere, tienen evidencia.
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
- PREGUNTA ABIERTA: exposición segura del token en desarrollo y efecto exacto en refresh activo.
