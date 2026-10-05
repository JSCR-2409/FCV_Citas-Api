---
id: HU-032
tipo: historia-de-usuario
titulo: "Recordar citas próximas"
estado: Completada
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
| CA-01 | Cumplido | Ejecución 9 contra la API real: 2 citas `APPROVED` seleccionadas con `selectedForReminder` | Más `IntegrationEndpointsTest.ca01_*` (4) |
| CA-02 | Cumplido | `WF-001-appointment-reminders.json`, JSON válido y sin credenciales | Verificado con grep |
| CA-03 | Cumplido | `IntegrationEndpointsTest.ca03_*` (2): el endpoint es de solo lectura | No existe ningún método de escritura |
| DoD | Cumplido | 4/4 | Ejecutado contra la API real por un túnel efímero, ya cerrado |

## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-04 — S5: WF-001 construido, validado y versionado; CA-01 pendiente de ejecucion real.
- 2026-10-04 — Cierre: ejecutado contra la API real a traves de un tunel efimero. CA-01 cumplido; estado `Completada`.

## Notas y decisiones

- ~~PREGUNTA ABIERTA: ventana de “próxima”, contenido del recordatorio y manejo de citas canceladas.~~
  **Resuelta el 2026-10-04.**

- **DECISIÓN — ventana.** Parametrica, 24 horas por defecto y acotada a 14 dias. Vive en el contrato y
  no en el workflow, de modo que cambiarla no exige reeditar el JSON.

- **DECISIÓN — citas canceladas y rechazadas.** No llegan nunca: el endpoint filtra por
  `status = APPROVED`, de modo que la exclusion es por codigo de estado y no por ausencia de
  cancelacion. Verificado en `ca01_cancelledAndRejectedAppointmentsAreNeverReminded`.

- **DECISIÓN — contenido.** Especialidad, profesional, fecha y hora, duracion y sede con su
  direccion. Sin numero de documento ni telefono: el recordatorio se envia por correo y no necesita
  mas.

- **DECISIÓN — antiduplicados.** Ventanas consecutivas no solapadas, por disparo diario a la misma
  hora, mas `misfirePolicy: skip` para que una ejecucion perdida no reenvie una ventana ya cubierta.
