---
id: EP-008
tipo: epica
titulo: "Automatizaciones posteriores"
estado: Pendiente de aprobación
historias:
  - "[[HU-032-recordar-citas-proximas]]"
  - "[[HU-033-notificar-cambios-de-estado]]"
  - "[[HU-034-resumir-operacion-diaria]]"
dependencias:
  - "[[HU-031-auditar-cambios-de-estado]]"
---

# EP-008 — Automatizaciones posteriores

## Objetivo

Incorporar workflows n8n de recordatorio, notificación de estados y resumen operativo sin modificar el núcleo funcional.

## Valor esperado

Extiende el laboratorio con automatización verificable y exportable.

## Actores

- USER.
- ADMIN.
- Equipo de operación.

## Alcance

- Workflows n8n versionados como JSON sin credenciales, compatibles con la instancia central del trainer.

## Fuera de alcance

- Credenciales reales, SMS/WhatsApp, modificación del núcleo funcional y despliegue autónomo de n8n.

## Reglas de negocio

- Recordatorio de cita próxima; aviso por cambio de estado; resumen por sede/estado; secretos solo en credenciales de la instancia.

## Dependencias

- [[HU-031-auditar-cambios-de-estado]]

## Historias de usuario

- [[HU-032-recordar-citas-proximas]]
- [[HU-033-notificar-cambios-de-estado]]
- [[HU-034-resumir-operacion-diaria]]

## Criterio de completitud de la épica

- [ ] Las tres HU están `Completada` con export JSON y evidencia de prueba sin secretos.
- [ ] Ningún workflow altera reglas del núcleo ni versiona credenciales.

## Riesgos e incógnitas

- Acceso a instancia, credenciales y canales Gmail están bajo control del trainer.
