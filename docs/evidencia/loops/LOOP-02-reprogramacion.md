---
tipo: evidencia-loop
ciclo: LOOP-02
estado: COMPLETED
hu: HU-024
---

# LOOP-02 — Retención doble al solicitar reprogramación

> Ciclo guiado avanzado de `prompts/goal-loop/LOOP_02_GUIADO_AVANZADO.md`. Registro reconstruido a
> partir del commit `d636356`.

## 1. Disparador

RN-10 exige que una solicitud de reprogramación **retenga las dos franjas**: la original, porque la
cita sigue siendo válida mientras el ADMIN no decida, y la propuesta, porque de lo contrario otro
paciente podría tomarla y la aprobación quedaría sin horario. La reprogramación no existía en
ninguna capa.

## 2. Meta verificable

La cita original `APPROVED` y futura conserva su franja; la nueva franja del mismo profesional y
especialidad queda retenida; la solicitud nace `PENDING`; un cambio de profesional o una cita no
elegible se rechazan sin dejar cambios parciales.

## 3. Estado observado

`ReschedulingTest.hu024_ca02_bothSlotsStayHeldWhilePending` y
`hu024_ca02_aHeldProposalIsNotOfferedToAnyoneElse`, en
[ReschedulingTest.java:122](../../../src/test/java/co/fcv/citas/appointment/ReschedulingTest.java).

## 4. Alcance del Builder

`PatientReschedulingController` y la migración `V9` del estado `CANCELLED` de reprogramación.
Fuera de alcance: la bandeja y la decisión del ADMIN, que son HU-029 y HU-030 y se trabajaron
por separado conforme a la regla de no fusionar las tres HU en un mismo bloque.

---

## Iteración 1

**Builder.** Implementó la solicitud retirando la franja original y asignando la propuesta, que es
la lectura intuitiva de "reprogramar".

**Verifier — FAIL.** Causa única: *la cita original pierde su franja mientras la solicitud está
pendiente*. Si el ADMIN rechaza, la cita queda `APPROVED` sin horario reservado y otro paciente
puede haberlo tomado. CA-02 exige que ambas estén retenidas.

```json
{
  "goal": "solicitar reprogramación retiene las dos franjas (HU-024 CA-02)",
  "iteration": 1,
  "builder": "completed",
  "backendTests": "fail",
  "frontendBuild": "skipped",
  "verifier": "FAIL",
  "cause": "la franja original se libera al crear la solicitud y el rechazo dejaría la cita sin horario",
  "result": "CONTINUE"
}
```

## Iteración 2

**Builder.** La propuesta se retiene asignándole **el mismo `appointment_id`** que la cita original,
en lugar de moverlo. Así ambos rangos quedan ocupados por la misma cita y ninguno se ofrece a nadie
más, sin necesidad de un estado intermedio en `professional_slots`.

**Verifier — PASS.** Con la solicitud pendiente, consultar disponibilidad no ofrece ninguna de las
dos franjas, y la cita conserva su horario original. Verificadas también las ramas de rechazo:
cita pasada, cita no `APPROVED`, cambio de profesional y solicitud de otro paciente.

```json
{
  "goal": "solicitar reprogramación retiene las dos franjas (HU-024 CA-02)",
  "iteration": 2,
  "builder": "completed",
  "backendTests": "pass",
  "frontendBuild": "pass",
  "verifier": "PASS",
  "result": "COMPLETED"
}
```

---

## 5. Escalamiento que sí ocurrió

Al aprobar una reprogramación **solapada** —mover una cita de 60 minutos media hora más tarde— la
liberación de la franja original borraba un slot que la cita seguía necesitando. Eso ya no era la
condición de este ciclo, sino de HU-030, así que **se detuvo el loop y se abrió el trabajo
correspondiente** en lugar de ampliar el alcance. La solución fue `releaseExcept`, que excluye del
rango liberado los slots que la cita conserva.

Esa es la condición de escalamiento funcionando: el Verifier no pidió arreglarlo aquí.

## 6. Por qué no bastaba un único prompt

La primera implementación era correcta según la lectura natural del requisito y pasaba una prueba
ingenua de "la cita cambió de hora". Solo una condición formulada sobre el **estado intermedio**
—mientras la solicitud está pendiente— revela el problema. Un prompt único no habría mirado ahí.

## 7. Resultado

`COMPLETED` en dos iteraciones. Commits `d636356` (backend) y `1f92a4a` (frontend).
