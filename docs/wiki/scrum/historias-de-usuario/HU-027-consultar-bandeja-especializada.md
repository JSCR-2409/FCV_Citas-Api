---
id: HU-027
tipo: historia-de-usuario
titulo: "Consultar bandeja especializada"
estado: Pendiente de aprobación
epica: "[[EP-007-operacion-administrativa-y-auditoria]]"
esfuerzo: Medio
sprint_sugerido: "Incremento V — Operación clínica simulada"
dependencias: ["[[HU-021-solicitar-cita-especializada]]"]
relacionadas: ["[[HU-028-resolver-solicitud-especializada]]"]
---

# HU-027 — Consultar bandeja especializada

## Historia de usuario
**COMO** ADMIN **QUIERO** consultar las citas especializadas REQUESTED con filtros operativos **PARA** decidirlas de forma ordenada.

## Alcance
- Bandeja de solicitudes REQUESTED filtrable por sede, profesional, especialidad y fecha.

## Fuera de alcance
- Ver/decidir citas generales, incluir solicitudes ya resueltas como pendientes o modificar desde lista sin decisión explícita.

## Reglas de negocio
- ADMIN ve solicitudes especializadas REQUESTED; filtros obligatorios del PRD; datos suficientes para decisión autorizada.

## Dependencias y relaciones
- Épica: [[EP-007-operacion-administrativa-y-auditoria]]
- Dependencias: [[HU-021-solicitar-cita-especializada]]
- Relacionada: [[HU-028-resolver-solicitud-especializada]]

## Esfuerzo
**Nivel:** Medio. Es una proyección ADMIN con filtros y exclusión correcta de estados.

## Tareas de desarrollo
- [ ] **T-01 — Definir consulta/filtros y atributos de bandeja.** Dificultad: Medio. Acordar orden/paginación si aplica.
- [ ] **T-02 — Implementar lectura protegida por rol y estado REQUESTED.** Dificultad: Medio. Optimizar por filtros relevantes.
- [ ] **T-03 — Integrar dashboard y pruebas de cola vacía/filtros.** Dificultad: Medio. Mostrar datos sin secretos.

## Criterios de aceptación
### CA-01 — Solo solicitudes pendientes
Dado un ADMIN, cuando abre la bandeja especializada, entonces ve citas especializadas en REQUESTED y no generales ni resueltas.
### CA-02 — Filtros operativos
Dado solicitudes con atributos distintos, cuando filtra por sede, profesional, especialidad o fecha, entonces solo permanecen las coincidencias.
### CA-03 — Restricción de rol
Dado USER o PROFESSIONAL, cuando intenta consultar la bandeja ADMIN, entonces backend lo rechaza.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas de estado, filtros, RBAC y UI aplicable.
- [ ] El contrato limita los atributos a la decisión y define vacío/error/paginación cuando se apruebe.
- [ ] La consulta cuenta con índices/migración Flyway si el esquema/consulta lo requiere.
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
- PREGUNTA ABIERTA: orden de atención y paginación de la bandeja.
