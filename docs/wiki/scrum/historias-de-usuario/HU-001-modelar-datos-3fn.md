---
id: HU-001
tipo: historia-de-usuario
titulo: "Modelar datos en 3FN"
estado: Pendiente de aprobación
epica: "[[EP-001-fundacion-de-datos-y-contrato-rest]]"
esfuerzo: Alto
sprint_sugerido: "Incremento I — Base especificable"
dependencias: []
relacionadas: ["[[HU-002-documentar-contrato-rest-inicial]]"]
---

# HU-001 — Modelar datos en 3FN

## Historia de usuario
**COMO** equipo de producto **QUIERO** definir y justificar un modelo relacional 3FN **PARA** soportar el producto sin duplicidad de datos ni ambigüedad estructural.

## Alcance
- Representar usuarios/roles, profesionales, catálogos, afiliación, disponibilidad, citas, slots, reprogramación, auditoría y tokens compatibles con el PRD.
- Documentar claves, únicas, cardinalidades, dependencias funcionales, índices candidatos y decisiones 1FN→3FN.

## Fuera de alcance
- SQL ejecutable, migraciones o adopción de la solución de referencia del trainer.

## Reglas de negocio
- N:M mediante tablas puente; datos de catálogo no se repiten; se preserva cita original durante reprogramación PENDING; 60 min son dos slots consecutivos.

## Dependencias y relaciones
- Épica: [[EP-001-fundacion-de-datos-y-contrato-rest]]
- Relacionada: [[HU-002-documentar-contrato-rest-inicial]]

## Esfuerzo
**Nivel:** Alto. Integra todas las entidades y reglas de integridad del producto sin implementar.

## Tareas de desarrollo
- [ ] **T-01 — Inventariar entidades y dependencias funcionales.** Dificultad: Medio. Trazar cada requisito de datos autorizado.
- [ ] **T-02 — Diseñar relaciones, claves y restricciones.** Dificultad: Alto. Justificar cardinalidades, únicos y retenciones.
- [ ] **T-03 — Documentar normalización y preguntas abiertas.** Dificultad: Medio. No usar el modelo de referencia sin habilitación.

## Criterios de aceptación
### CA-01 — Cobertura del dominio de datos
Dado el PRD y requisitos 3FN, cuando se revise el modelo, entonces representa todas las capacidades listadas sin listas en columnas.
### CA-02 — Normalización justificada
Dado cada relación, cuando se inspeccionen dependencias, entonces las decisiones 1FN, 2FN y 3FN, claves y cardinalidades son trazables.
### CA-03 — Reservas y reprogramación preservadas
Dado una cita de 60 min o una reprogramación pendiente, cuando se examine el diseño, entonces soporta slots consecutivos y conserva la cita original hasta decisión ADMIN.

## Definition of Done
- [ ] CA-01 a CA-03 tienen evidencia de revisión.
- [ ] Existe diagrama ER y justificación de claves, cardinalidades, dependencias e índices candidatos sin usar datos reales.
- [ ] Las preguntas sobre snapshots, concurrencia y retenciones están documentadas para aprobación.
- [ ] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

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
- PREGUNTA ABIERTA: qué atributos son snapshots frente a FK y cómo se materializa la exclusión de reserva.
