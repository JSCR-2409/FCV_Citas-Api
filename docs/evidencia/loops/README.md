---
tipo: evidencia-s4
estado: Registrado
---

# Evidencia de ciclos Builder/Verifier — S4

Cada ciclo de esta carpeta corresponde a **una condición reproducible**, conforme a la regla de
tamaño de `PLAN_AJUSTADO_S3_S5.md`: un loop no abarca una HU completa ni una épica, sino una prueba,
un error de contrato o una transición. El presupuesto es de **dos iteraciones**; a la tercera se
escala.

## Contrato del ciclo

| Rol | Puede | No puede |
|---|---|---|
| **Builder** | Leer la condición fallida, aplicar el cambio mínimo, ejecutar las pruebas directas | Ampliar el alcance, cambiar contrato o esquema sin decisión aprobada |
| **Verifier** | Inspeccionar HU/DoD, el diff y los resultados; devolver PASS o FAIL con **una** causa | Implementar, corregir, o exigir la suite completa si la condición no la requiere |

## Condiciones de parada y escalamiento

- **Parada por éxito:** el Verifier devuelve PASS con evidencia de la condición y sin cambios parciales.
- **Parada por presupuesto:** dos iteraciones agotadas sobre la misma condición.
- **Escalamiento humano:** una PREGUNTA ABIERTA, una migración no aprobada, o un cambio REST que
  afecte al otro repositorio sin contrato acordado.

## Ciclos registrados

| Ciclo | Condición | Iteraciones | Resultado |
|---|---|---|---|
| [LOOP-01](LOOP-01-doble-reserva.md) | Doble reserva sobre la misma franja (HU-020) | 2 | COMPLETED |
| [LOOP-02](LOOP-02-reprogramacion.md) | Retención doble al solicitar reprogramación (HU-024) | 2 | COMPLETED |
| [LOOP-03](LOOP-03-reto-independiente.md) | Revocación de sesiones al recuperar contraseña (HU-006) | 2 | COMPLETED |

Los tres son ciclos reales: la condición falló primero, y el log recoge el diagnóstico y el cambio
de cada iteración. LOOP-01 y LOOP-02 se reconstruyen a partir del trabajo de la sesión en que
ocurrieron, con el commit y la prueba que los respaldan; LOOP-03 se registró mientras ocurría.

## Formato de observabilidad

Cada ciclo incluye un bloque JSON por iteración con la forma que pide `GUIA_SESIONES_S2_S6.md`:

```json
{
  "goal": "...",
  "iteration": 1,
  "builder": "completed",
  "backendTests": "fail",
  "frontendBuild": "skipped",
  "verifier": "FAIL",
  "result": "CONTINUE"
}
```

`skipped` no es un resultado neutro disfrazado: significa que la condición no toca ese repositorio,
y el Verifier no debe exigir una verificación que no aporta evidencia sobre la condición.
