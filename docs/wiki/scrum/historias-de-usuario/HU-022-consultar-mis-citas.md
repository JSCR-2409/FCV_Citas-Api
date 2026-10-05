---
id: HU-022
tipo: historia-de-usuario
titulo: "Consultar mis citas"
estado: Completada
epica: "[[EP-005-ciclo-de-citas-del-usuario]]"
esfuerzo: Medio
sprint_sugerido: "Incremento IV — Reserva del usuario"
dependencias: ["[[HU-020-reservar-cita-general]]", "[[HU-021-solicitar-cita-especializada]]"]
relacionadas: ["[[HU-023-cancelar-cita]]", "[[HU-024-solicitar-reprogramacion]]"]
---

# HU-022 — Consultar mis citas

## Historia de usuario
**COMO** USER autenticado **QUIERO** consultar y filtrar mis citas por estado y fecha **PARA** conocer sus detalles y actuar cuando corresponda.

## Alcance
- Lista/detalle propio con sede, profesional, especialidad, fecha/hora, duración, estado y motivo de rechazo cuando exista.

## Fuera de alcance
- Consultar citas ajenas, modificar estados desde la vista o ver historia clínica.

## Reglas de negocio
- Ownership USER; filtro por estado/fecha; motivo de rechazo visible cuando exista.

## Dependencias y relaciones
- Épica: [[EP-005-ciclo-de-citas-del-usuario]]
- Dependencias: [[HU-020-reservar-cita-general]], [[HU-021-solicitar-cita-especializada]]
- Relacionadas: [[HU-023-cancelar-cita]], [[HU-024-solicitar-reprogramacion]]

## Esfuerzo
**Nivel:** Medio. Es una proyección autorizada con filtros y detalles condicionados por estado.

## Tareas de desarrollo
- [ ] **T-01 — Definir lectura, filtros y atributos de detalle.** Dificultad: Medio. Acordar paginación si se requiere.
- [ ] **T-02 — Implementar proyección con ownership y motivo condicionado.** Dificultad: Medio. Evitar exponer terceros.
- [ ] **T-03 — Integrar listado/detalle y estados UI.** Dificultad: Medio. Validar empty/loading/error.

## Criterios de aceptación
### CA-01 — Datos mínimos
Dado un USER con citas, cuando consulta las propias, entonces cada resultado muestra sede, profesional, especialidad, fecha/hora, duración y estado.
### CA-02 — Filtros
Dado filtros de estado y/o fecha, cuando los aplica, entonces solo se muestran sus citas coincidentes.
### CA-03 — Motivo condicionado y ownership
Dado una cita REJECTED, cuando USER la ve, entonces se muestra motivo; dado identificador de otro USER, no se muestran sus citas.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de proyección, filtros/ownership y UI aplicable.
- [ ] El contrato limita atributos a los autorizados y maneja vacío/error de forma verificable.
- [ ] La integración directa frontend/backend está comprobada para los filtros aprobados.
- [ ] La trazabilidad Scrum está actualizada.

## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumplido | ProfileAndMyAppointmentsTest.hu022_ca01_* | Filtros status, from y to aplicados en SQL |
| CA-02 | Cumplido | hu022_ca02_* (2) | Filtros status, from y to aplicados en SQL |
| CA-03 | Cumplido | hu022_ca03_* (2) | Filtros status, from y to aplicados en SQL |
| DoD | Cumplido | 5 pruebas backend + 1 frontend del motivo | 4/4 |

## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-02 — S4: CA verificados con pruebas automatizadas y UI conectada; estado `Completada`.

## Notas y decisiones

- ~~PREGUNTA ABIERTA: orden, paginación y definición de filtro por fecha inclusivo.~~
  **Resuelta el 2026-10-02 (S4).**

- **DECISIÓN — orden.** Descendente por fecha de inicio. La UI separa «Próximas» e «Historial» a
  partir de esa lista, de modo que el orden del contrato no decide la presentación.

- **DECISIÓN — filtro de fecha inclusivo en ambos extremos.** `from` y `to` son fechas, no instantes.
  El rango se traduce a `[from 00:00, to+1día 00:00)`, así que una cita de las 16:00 del día `to`
  queda dentro. Tratar `to` como medianoche excluiría casi todas las citas de ese día, que es
  exactamente lo que el usuario no espera. Verificado en `hu022_ca02_theDateFilterIsInclusiveOnBothEnds`.

- **DECISIÓN — sin paginación.** Un paciente tiene decenas de citas, no miles. Añadir paginación
  ahora complicaría el contrato sin resolver un problema real; si aparece, se añade como cambio de
  contrato con evidencia en ambos repositorios.

- **DECISIÓN — filtro de estado múltiple.** `status` admite varios códigos separados por coma, para
  que la vista de citas activas se resuelva en una sola llamada en lugar de dos.
