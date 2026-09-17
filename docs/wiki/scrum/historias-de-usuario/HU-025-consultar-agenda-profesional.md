---
id: HU-025
tipo: historia-de-usuario
titulo: "Consultar agenda profesional"
estado: Pendiente de aprobación
epica: "[[EP-006-operacion-del-profesional]]"
esfuerzo: Medio
sprint_sugerido: "Incremento V — Operación clínica simulada"
dependencias: ["[[HU-020-reservar-cita-general]]", "[[HU-021-solicitar-cita-especializada]]"]
relacionadas: ["[[HU-026-cerrar-atencion]]"]
---

# HU-025 — Consultar agenda profesional

## Historia de usuario
**COMO** PROFESSIONAL **QUIERO** consultar mis citas APPROVED por día, semana y sede **PARA** organizar la atención sin ver datos fuera de mis citas.

## Alcance
- Agenda propia filtrable por día/semana/sede con citas APPROVED.

## Fuera de alcance
- Ver pacientes o citas de otro profesional, historias clínicas o solicitudes aún no aprobadas.

## Reglas de negocio
- Solo citas APPROVED propias; ownership estricto; filtros día/semana/sede.

## Dependencias y relaciones
- Épica: [[EP-006-operacion-del-profesional]]
- Dependencias: [[HU-020-reservar-cita-general]], [[HU-021-solicitar-cita-especializada]]
- Relacionada: [[HU-026-cerrar-atencion]]

## Esfuerzo
**Nivel:** Medio. Requiere proyección con ownership, filtros temporales y privacidad.

## Tareas de desarrollo
- [ ] **T-01 — Definir atributos mínimos y filtros de agenda.** Dificultad: Medio. Proteger datos de USER.
- [ ] **T-02 — Implementar consulta con rol/ownership/estado.** Dificultad: Medio. Excluir REQUESTED y ajenas.
- [ ] **T-03 — Integrar dashboard calendario y pruebas de aislamiento.** Dificultad: Medio. Aplicar diseño aprobado.

## Criterios de aceptación
### CA-01 — Agenda filtrable
Dado un PROFESSIONAL con citas APPROVED, cuando filtra por día, semana o sede, entonces ve únicamente coincidencias propias.
### CA-02 — Estado limitado
Dado una solicitud REQUESTED/REJECTED u otra no APPROVED, cuando se consulta agenda visible, entonces no aparece como cita de atención aprobada.
### CA-03 — Privacidad
Dado otro profesional, cuando intenta acceder a agenda ajena, entonces backend no entrega sus citas ni datos de sus USER.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de filtros, estado y ownership más UI aplicable.
- [ ] Contrato minimiza datos expuestos y la interfaz maneja vacío/error/carga.
- [ ] No se introduce historia clínica ni datos no autorizados.
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
- PREGUNTA ABIERTA: conjunto exacto de atributos de USER visible a su profesional para la cita.
