---
id: HU-032
tipo: historia-de-usuario
titulo: "Recordar citas próximas"
estado: Pendiente de aprobación
epica: "[[EP-008-automatizaciones-posteriores]]"
esfuerzo: Alto
sprint_sugerido: "Incremento VI — Automatizaciones posteriores"
dependencias: ["[[HU-020-reservar-cita-general]]", "[[HU-028-resolver-solicitud-especializada]]"]
relacionadas: ["[[HU-034-resumir-operacion-diaria]]"]
---

# HU-032 — Recordar citas próximas

## Historia de usuario
**COMO** USER con una cita próxima **QUIERO** recibir un recordatorio automatizado **PARA** asistir a mi cita programada.

## Alcance
- Workflow n8n para recordatorio Gmail de citas próximas, exportado/versionado como JSON sin credenciales.

## Fuera de alcance
- SMS/WhatsApp, credenciales en repositorio, modificar estados de cita o cambiar núcleo funcional.

## Reglas de negocio
- Automatización posterior; instancia central del trainer; secretos en credenciales n8n/Google Cloud, nunca en JSON versionado.

## Dependencias y relaciones
- Épica: [[EP-008-automatizaciones-posteriores]]
- Dependencias: [[HU-020-reservar-cita-general]], [[HU-028-resolver-solicitud-especializada]]
- Relacionada: [[HU-034-resumir-operacion-diaria]]

## Esfuerzo
**Nivel:** Alto. Requiere fuente segura de próximas citas, workflow y operación externa del trainer.

## Tareas de desarrollo
- [ ] **T-01 — Definir fuente/criterio de “próxima” autorizado.** Dificultad: Alto. No deducir ventana temporal.
- [ ] **T-02 — Configurar y probar workflow n8n sin credenciales versionadas.** Dificultad: Alto. Exportar JSON al directorio autorizado.
- [ ] **T-03 — Registrar evidencia sintética de envío/resultado.** Dificultad: Medio. No incluir PII ni tokens.

## Criterios de aceptación
### CA-01 — Selección autorizada
Dado una cita aprobada que cumpla el criterio aprobado de proximidad, cuando corre el workflow, entonces se selecciona para recordatorio.
### CA-02 — Workflow exportable
Dado el workflow configurado en la instancia trainer, cuando se exporta, entonces el JSON puede versionarse bajo `automations/n8n/` sin credenciales.
### CA-03 — Sin alteración del núcleo
Dado una ejecución, cuando finaliza, entonces no cambia estado, reserva ni reglas de negocio de la cita.

## Definition of Done
- [ ] CA-01 a CA-03 tienen evidencia de prueba con datos sintéticos y workflow exportado.
- [ ] Credenciales Gmail/n8n no aparecen en JSON, logs versionados ni documentación Scrum.
- [ ] La fuente API/consulta necesaria tiene contrato autorizado y control de acceso adecuado.
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
- PREGUNTA ABIERTA: ventana de “próxima”, contenido del recordatorio y manejo de citas canceladas.
