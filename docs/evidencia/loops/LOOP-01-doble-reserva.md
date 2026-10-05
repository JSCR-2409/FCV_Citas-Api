---
tipo: evidencia-loop
ciclo: LOOP-01
estado: COMPLETED
hu: HU-020
---

# LOOP-01 — Doble reserva sobre la misma franja

> Ciclo guiado sencillo de `prompts/goal-loop/LOOP_01_GUIADO_SIMPLE.md`. Registro reconstruido a
> partir del trabajo del commit `d58ba9f`, con la prueba y el diff que lo respaldan.

## 1. Disparador

La prueba `hu020_ca01_generalBookingIsApprovedWithoutAdmin` pasaba, pero no existía ninguna que
intentara reservar dos veces la misma franja. RN-01 lo prohíbe de forma explícita: *"ninguna cita
puede ocupar slots ya reservados/retenidos"*.

## 2. Meta verificable

Una reserva conserva su franja y el segundo intento recibe el conflicto definido por contrato, sin
cambios parciales: no se crea una segunda cita ni se reasigna el slot.

## 3. Estado observado

`AppointmentBookingRulesTest.hu020_ca02_secondBookingOnTheSameSlotIsRejected`, en
[AppointmentBookingRulesTest.java:107](../../../src/test/java/co/fcv/citas/appointment/AppointmentBookingRulesTest.java).

## 4. Alcance del Builder

`AppointmentBookingController` únicamente. No se tocaron contrato, esquema ni UI.

---

## Iteración 1

**Builder.** Escribió la prueba de doble reserva y la ejecutó contra la implementación existente.
Falló, pero no por la razón esperada: la reserva reclamaba las franjas con un `UPDATE ... JOIN`,
sintaxis exclusiva de MySQL, de modo que en H2 la prueba no llegaba a ejercer la regla.

**Verifier — FAIL.** Causa única: *la condición no es verificable porque el reclamo de franjas usa
sintaxis propia de MySQL*. No se pronunció sobre la regla de negocio, porque todavía no había
evidencia sobre ella.

```json
{
  "goal": "una franja reservada no se puede reservar dos veces (HU-020 CA-02)",
  "iteration": 1,
  "builder": "completed",
  "backendTests": "fail",
  "frontendBuild": "skipped",
  "verifier": "FAIL",
  "cause": "UPDATE ... JOIN es sintaxis de MySQL y la condición no se puede ejercer",
  "result": "CONTINUE"
}
```

## Iteración 2

**Builder.** Reescribió el reclamo como un único `UPDATE` con subconsulta, portable y con la guarda
en la propia condición:

```java
int claimed = db.update("UPDATE professional_slots SET appointment_id=? WHERE appointment_id IS NULL"
    + " AND start_at>=? AND start_at<? AND availability_block_id IN"
    + " (SELECT id FROM availability_blocks WHERE professional_id=? AND location_id=?)",
    appointment, start, end, pro, loc);
if (claimed != needed) throw new IllegalStateException("conflicto de concurrencia");
```

La guarda es `appointment_id IS NULL` dentro del mismo `UPDATE`: si otra reserva se adelantó, el
número de filas afectadas no coincide y la transacción se deshace entera. Comprobar antes y escribir
después dejaría una ventana entre ambas operaciones.

**Verifier — PASS.** La cita original conserva su franja, el segundo intento recibe `409` y no se
crea una segunda cita. Verificado también el caso de 60 minutos con el segundo slot ocupado
(`hu019_ca02_sixtyMinutesIsRejectedWhenTheNextSlotIsTaken`), que es la misma condición con dos
franjas.

```json
{
  "goal": "una franja reservada no se puede reservar dos veces (HU-020 CA-02)",
  "iteration": 2,
  "builder": "completed",
  "backendTests": "pass",
  "frontendBuild": "skipped",
  "verifier": "PASS",
  "result": "COMPLETED"
}
```

---

## 5. Por qué no bastaba un único prompt

El primer fallo no era el que se buscaba. Un prompt único habría concluido "la regla no funciona" y
habría cambiado la lógica de negocio, cuando el problema real era que la condición no se podía
observar. La separación Builder/Verifier obligó a nombrar **una** causa por iteración, y eso evitó
corregir lo que no estaba roto.

## 6. Resultado

`COMPLETED` en dos iteraciones, dentro del presupuesto. Commit `d58ba9f`.
