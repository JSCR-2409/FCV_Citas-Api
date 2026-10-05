---
id: HU-006
tipo: historia-de-usuario
titulo: "Recuperar contraseña"
estado: Completada
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
| CA-01 | Cumplido | PasswordRecoveryTest.ca01_* (4) | Canal de laboratorio documentado: app.recovery.expose-token |
| CA-02 | Cumplido | PasswordRecoveryTest.ca02_* (3) | Canal de laboratorio documentado: app.recovery.expose-token |
| CA-03 | Cumplido | PasswordRecoveryTest.ca03_* (3) | Canal de laboratorio documentado: app.recovery.expose-token |
| DoD | Cumplido | 11 pruebas + verificación en vivo contra MySQL | 4/4 |

## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-02 — S4: CA verificados con pruebas automatizadas y UI conectada; estado `Completada`.

## Notas y decisiones

- ~~PREGUNTA ABIERTA: exposición segura del token en desarrollo y efecto exacto en refresh activo.~~
  **Resuelta el 2026-10-02 (S4).** Las dos partes:

- **DECISIÓN — canal de desarrollo.** El token viaja en la respuesta de `POST /api/auth/recovery/request`
  **solo** si `app.recovery.expose-token` es `true`. El valor por defecto en `application.yml` es
  `false`, y se habilita únicamente en `docker-compose.yml` y en el perfil de pruebas. RF-03 lo admite
  de forma explícita porque el envío de correo es opcional. Activar un envío real no cambia el
  contrato: el campo simplemente deja de venir y el usuario pega el código en el paso 2.

- **DECISIÓN — efecto sobre el refresh activo.** Consumir el token **revoca todas las sesiones vivas**
  de la cuenta. El motivo por el que alguien recupera su contraseña suele ser que la cuenta está
  comprometida; dejar vivo el refresh del atacante vaciaría de sentido la recuperación. Verificado en
  `ca02_changingThePasswordRevokesLiveSessions` y documentado en
  [LOOP-03](../../../evidencia/loops/LOOP-03-reto-independiente.md).

- **DECISIÓN — no enumeración.** La solicitud responde `200` con el mismo cuerpo exista o no la
  cuenta. Devolver `404` convertiría el endpoint en un oráculo para enumerar los correos registrados.
  Esto cambia el comportamiento anterior, en el que la UI mostraba «No encontramos una cuenta activa
  con ese correo».

- **Defecto de seguridad cerrado.** El endpoint anterior, `POST /api/auth/password-reset`, cambiaba la
  contraseña de cualquier cuenta conociendo solo su email, sin ninguna prueba de posesión del buzón.
  Se retiró. `theUnauthenticatedPasswordResetEndpointNoLongerExists` fija que no vuelva.
