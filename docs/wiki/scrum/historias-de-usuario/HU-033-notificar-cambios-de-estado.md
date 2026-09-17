---
id: HU-033
tipo: historia-de-usuario
titulo: "Notificar cambios de estado"
estado: Pendiente de aprobación
epica: "[[EP-008-automatizaciones-posteriores]]"
esfuerzo: Alto
sprint_sugerido: "Incremento VI — Automatizaciones posteriores"
dependencias: ["[[HU-031-auditar-cambios-de-estado]]"]
relacionadas: ["[[HU-032-recordar-citas-proximas]]"]
---

# HU-033 — Notificar cambios de estado

## Historia de usuario
**COMO** USER afectado por un cambio de estado **QUIERO** recibir una notificación automatizada **PARA** conocer el resultado de mi cita o solicitud.

## Alcance
- Workflow n8n por webhook y Gmail para cambios de estado, exportado como JSON sin credenciales.

## Fuera de alcance
- Cambiar la transición de estado, notificar por SMS/WhatsApp o exponer el webhook sin controles aprobados.

## Reglas de negocio
- Se apoya en auditoría; secrets no versionados; fuente de cambio debe distinguir SYSTEM/USER/ADMIN si es relevante al mensaje.

## Dependencias y relaciones
- Épica: [[EP-008-automatizaciones-posteriores]]
- Dependencias: [[HU-031-auditar-cambios-de-estado]]
- Relacionada: [[HU-032-recordar-citas-proximas]]

## Esfuerzo
**Nivel:** Alto. Conecta eventos de dominio, webhook, correo y protección de datos.

## Tareas de desarrollo
- [ ] **T-01 — Definir evento/webhook y datos mínimos de notificación.** Dificultad: Alto. Aplicar autorización/autenticación aprobada.
- [ ] **T-02 — Configurar workflow n8n y Gmail con credenciales externas.** Dificultad: Alto. Exportar JSON sin secretos.
- [ ] **T-03 — Probar eventos sintéticos y ausencia de mutación de dominio.** Dificultad: Medio. Registrar evidencia segura.

## Criterios de aceptación
### CA-01 — Evento de cambio
Dado un cambio de estado auditado, cuando se publica el evento/webhook autorizado, entonces el workflow puede identificar cita, estado y destinatario permitido.
### CA-02 — Notificación sin secretos
Dado el workflow exportado, cuando se revisa su JSON y salida de prueba, entonces no contienen credenciales, tokens ni información no autorizada.
### CA-03 — Núcleo intacto
Dado una falla o éxito del workflow, cuando termina, entonces no modifica la transición ni los registros de reserva/auditoría del núcleo.

## Definition of Done
- [ ] CA-01 a CA-03 tienen evidencia con datos sintéticos, webhook controlado y JSON exportado.
- [ ] Credenciales Gmail/n8n/webhook se mantienen fuera del repositorio y se documenta configuración externa requerida.
- [ ] El contrato del evento está trazado a auditoría y contiene controles de acceso aprobados.
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
- PREGUNTA ABIERTA: eventos/notificaciones destinatarios y autenticación concreta del webhook.
