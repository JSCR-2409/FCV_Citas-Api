---
id: HU-034
tipo: historia-de-usuario
titulo: "Resumir operación diaria"
estado: Completada
epica: "[[EP-008-automatizaciones-posteriores]]"
esfuerzo: Alto
sprint_sugerido: "Incremento VI — Automatizaciones posteriores"
dependencias: ["[[HU-031-auditar-cambios-de-estado]]"]
relacionadas: ["[[HU-032-recordar-citas-proximas]]"]
---

# HU-034 — Resumir operación diaria

## Historia de usuario
**COMO** equipo de operación autorizado **QUIERO** recibir un resumen diario de citas por sede y estado **PARA** conocer la operación sintética sin consultar registros individuales.

## Alcance
- Workflow n8n de resumen por sede/estado, con datos agregados autorizados y JSON sin credenciales.

## Fuera de alcance
- Reportes clínicos, exportación de PII, modificación de citas o credenciales versionadas.

## Reglas de negocio
- Caso adicional S5/S6; resume por sede/estado; usa datos sintéticos y acceso autorizado; no altera núcleo.

## Dependencias y relaciones
- Épica: [[EP-008-automatizaciones-posteriores]]
- Dependencias: [[HU-031-auditar-cambios-de-estado]]
- Relacionada: [[HU-032-recordar-citas-proximas]]

## Esfuerzo
**Nivel:** Alto. Requiere agregación fiable, acceso operativo controlado y workflow externo.

## Tareas de desarrollo
- [ ] **T-01 — Definir período diario, destinatario y agregados permitidos.** Dificultad: Alto. No incluir PII sin aprobación.
- [ ] **T-02 — Implementar fuente agregada y workflow n8n.** Dificultad: Alto. Mantener reglas de acceso.
- [ ] **T-03 — Exportar JSON y verificar salida sintética.** Dificultad: Medio. Comprobar que no cambia dominio.

## Criterios de aceptación
### CA-01 — Agregación por sede/estado
Dado datos de citas autorizados para el período definido, cuando corre el workflow, entonces produce conteos agrupados por sede y estado.
### CA-02 — Privacidad operativa
Dado el resumen, cuando se revisa, entonces no contiene datos de paciente/profesional no autorizados ni credenciales.
### CA-03 — Artefacto versionable
Dado el workflow validado, cuando se exporta, entonces su JSON se puede versionar en `automations/n8n/` sin secretos y sin alterar el núcleo.

## Definition of Done
- [ ] CA-01 a CA-03 tienen evidencia con datos sintéticos, agregación correcta y JSON exportado.
- [ ] La consulta/fuente cuenta con contrato, mínimo privilegio e índices/migración Flyway si requiere cambios.
- [ ] Credenciales y destinatarios se configuran fuera del repositorio; no hay PII en artefactos versionados.
- [ ] La trazabilidad Scrum está actualizada.

## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumplido | Ejecución 10 contra la API real: conteos por sede, estado y especialidad | Más `IntegrationEndpointsTest.hu034_*` (3) |
| CA-02 | Cumplido | `hu034_theSummaryCarriesNoPatientData`: el resumen son conteos, sin nombres ni correos | Verificado en prueba |
| CA-03 | Cumplido | `WF-003-daily-operational-summary.json`, JSON válido y sin secretos | El endpoint es de solo lectura |
| DoD | Cumplido | 4/4 | Ejecutado contra la API real por un túnel efímero, ya cerrado |

## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-04 — S6: WF-003 construido y versionado como bonus; CA-01 pendiente de ejecucion real.
- 2026-10-04 — Cierre: ejecutado contra la API real a traves de un tunel efimero. CA-01 cumplido; estado `Completada`.

## Notas y decisiones

- ~~PREGUNTA ABIERTA: período de corte, destinatario y alcance de los agregados del resumen.~~
  **Resuelta el 2026-10-04.**

- **DECISIÓN — período de corte.** El día natural completo, `[date 00:00, date+1 00:00)`, con el día
  en curso por defecto y `date` opcional para consultar otro. El disparo diario es a las 19:00.

- **DECISIÓN — alcance de los agregados.** Tres cortes: total por sede y estado, y distribución por
  especialidad. El endpoint devuelve **conteos, no citas**: un resumen no necesita el nombre ni el
  correo de ningún paciente. Verificado en `hu034_theSummaryCarriesNoPatientData`.

- **DECISIÓN — destinatario.** Un buzón de operación, configurado en el nodo Gmail y no en el
  contrato: cambiarlo no debe tocar el backend. El JSON versionado lleva un marcador de posición.

- **DECISIÓN — incidencias.** Si la API no responde tras tres reintentos, se envía un correo de
  incidencia. Un resumen que no llega se confundiría con un día sin actividad.
