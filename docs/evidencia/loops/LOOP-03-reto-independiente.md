---
tipo: evidencia-loop
ciclo: LOOP-03
estado: COMPLETED
hu: HU-006
---

# LOOP-03 — Revocación de sesiones al recuperar la contraseña

> Reto independiente de `prompts/goal-loop/LOOP_03_RETO_INDEPENDIENTE.md`. A diferencia de los dos
> anteriores, este ciclo **se registró mientras ocurría**, en la sesión S4.

El prompt pide los diez elementos del reto. Van numerados.

## 1. Disparador

Al implementar HU-006 apareció una pregunta que los CA no responden de forma directa: cambiar la
contraseña, ¿qué pasa con las sesiones abiertas? La nota de la HU lo registra como **PREGUNTA
ABIERTA**: *"exposición segura del token en desarrollo y efecto exacto en refresh activo"*.

La respuesta elegida no es una inferencia libre: si la cuenta estaba comprometida —que es el motivo
por el que alguien recupera su contraseña—, dejar vivo el refresh del atacante vacía de sentido la
recuperación. Se decidió revocar, y se escribió la condición que lo comprueba.

## 2. Meta verificable

Tras consumir un token de recuperación, un `refreshToken` emitido **antes** del cambio deja de
servir: `POST /api/auth/refresh` responde `401`.

## 3. Estado observado y persistente

`PasswordRecoveryTest.ca02_changingThePasswordRevokesLiveSessions`, en
[PasswordRecoveryTest.java:152](../../../src/test/java/co/fcv/citas/auth/PasswordRecoveryTest.java).
El estado persistente es la columna `refresh_sessions.revoked`.

## 4. Alcance del Builder

`PasswordRecoveryService` y `RefreshSessionRepository`. Fuera de alcance: el resto del flujo de
recuperación, que ya estaba en verde, y la UI.

## 5. Evidencia que exige el Verifier

Las 11 pruebas de `PasswordRecoveryTest` y, en particular, que el `401` provenga de la revocación y
no de otra causa.

## 6. Presupuesto

Dos iteraciones.

---

## Iteración 1

**Builder.** Revocó las sesiones con un `UPDATE` directo, que es la vía más corta:

```java
db.update("UPDATE refresh_sessions SET revoked=TRUE WHERE user_id=? AND revoked=FALSE", userId);
```

Resultado: `10 de 11` pruebas en verde. La única que falló fue precisamente la condición del ciclo.

```text
[ERROR] PasswordRecoveryTest.ca02_changingThePasswordRevokesLiveSessions:167
        Status expected:<401> but was:<200>
[ERROR] Tests run: 11, Failures: 1, Errors: 0, Skipped: 0
```

**Verifier — FAIL.** Causa única: *el `UPDATE` por JDBC no se refleja en las entidades que JPA ya
tiene cargadas*. `findByTokenHash` devuelve la instancia gestionada en la sesión de Hibernate, con
`revoked=false` en memoria, así que el filtro `!s.isRevoked()` la acepta.

El Verifier añadió una observación que el Builder no debía resolver: en producción cada petición
abre su propia transacción, de modo que el `UPDATE` **sí** sería visible y el comportamiento
parecería correcto. El fallo es una inconsistencia real dentro de una transacción, no un artefacto
de la prueba. Esa distinción es la que decide si se corrige el código o la prueba.

```json
{
  "goal": "cambiar la contraseña revoca las sesiones vivas (HU-006 CA-02)",
  "iteration": 1,
  "builder": "completed",
  "backendTests": "fail",
  "frontendBuild": "skipped",
  "verifier": "FAIL",
  "cause": "el UPDATE por JDBC no invalida las entidades que JPA tiene en la sesión",
  "result": "CONTINUE"
}
```

## Iteración 2

**Builder.** Revocó a través del repositorio, de modo que el estado en memoria y el de la base
coinciden:

```java
var live = sessions.findByUserIdAndRevokedFalse(userId);
live.forEach(RefreshSession::revoke);
sessions.saveAll(live);
```

**Verifier — PASS.** `11 de 11`. El `refreshToken` anterior devuelve `401`; el flujo completo
—solicitud, consumo, reutilización rechazada, token vencido— sigue en verde. Suite completa del
backend: `153` pruebas, 0 fallos.

```json
{
  "goal": "cambiar la contraseña revoca las sesiones vivas (HU-006 CA-02)",
  "iteration": 2,
  "builder": "completed",
  "backendTests": "pass",
  "frontendBuild": "pass",
  "verifier": "PASS",
  "result": "COMPLETED"
}
```

---

## 7. Condición de parada

PASS del Verifier con la condición demostrada y sin cambios parciales. Se cumplió en la segunda
iteración.

## 8. Condición de escalamiento humano

Estaba definida así: si la revocación exigiera un cambio en el contrato de `/api/auth/refresh` o en
el esquema de `refresh_sessions`, se detiene y se escala. No fue necesario: la corrección quedó
dentro de la capa de persistencia.

## 9. Log de ejecución

Los dos bloques JSON de arriba, más la salida de Surefire de la iteración 1.

## 10. Por qué no bastaba un único prompt

Porque el fallo **no estaba donde parecía**. Un prompt único, ante `expected 401 but was 200`, tiene
dos salidas naturales: dar por bueno que "en producción funciona" y relajar la prueba, o cambiar la
lógica de revocación, que ya era correcta. Ninguna de las dos arregla nada.

Lo que resolvió el ciclo fue obligar al Verifier a nombrar la causa **sin poder tocar el código**.
Eso forzó el diagnóstico —mezcla de escrituras JDBC con lecturas JPA en la misma transacción— y
solo entonces el cambio mínimo fue evidente. El mismo diagnóstico explicó, en el mismo bloque de
trabajo, otro defecto independiente: `PATCH /me` escribía por JPA y releía por JDBC, y devolvía los
valores anteriores al cambio.

Un ciclo con dos roles separados produjo un diagnóstico reutilizable. Un prompt único habría
producido un parche.

## Resultado

`COMPLETED` en dos iteraciones. Commit `563fe09`.
