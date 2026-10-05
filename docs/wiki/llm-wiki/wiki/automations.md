# Automatizaciones

Actualizado: 2026-10-04.

Los tres workflows n8n viven como JSON bajo `automations/n8n/`, construidos por MCP contra la
instancia del trainer. **Ninguno lleva credenciales**, y los tres quedan **inactivos**.

## HECHO — Workflows versionados

| JSON | HU | Nodos | Disparador | Estado HU |
|---|---|---|---|---|
| `WF-001-appointment-reminders.json` | HU-032 | 7 | Schedule diario 08:00 | `En validación` |
| `WF-002-status-notifications.json` | HU-033 | 12 | Webhook desde `citas-api` | `Completada` |
| `WF-003-daily-operational-summary.json` | HU-034 | 6 | Schedule diario 19:00 | `En validación` |

## DECISIÓN — Autenticación de las automatizaciones

Token de servicio en la cabecera `X-Integration-Token`, no un JWT de usuario. n8n no es una persona:
no tiene perfil, no renueva sesión y no debe quedar atado a la cuenta de nadie, porque desactivar a
ese usuario rompería la automatización en silencio.

El token concede `ROLE_INTEGRATION`, una autoridad **propia** que no reutiliza `ADMIN`: no abre los
endpoints administrativos ni los del paciente. La comparación es en tiempo constante y, si la variable
no está definida, los endpoints de integración quedan cerrados.

## DECISIÓN — Los endpoints de integración son de solo lectura

`GET /api/v1/integrations/appointments/upcoming` y `GET /api/v1/integrations/daily-summary` no tienen
ningún método de escritura. Así el CA-03 de HU-032 —«no cambia estado, reserva ni reglas de negocio»—
se cumple por construcción y no por disciplina.

## DECISIÓN — El criterio de «próxima» vive en el contrato

Ventana paramétrica, 24 horas por defecto, acotada a 14 días. No se deduce en el workflow: cambiarla
no exige reeditar el JSON.

## DECISIÓN — Estrategia antiduplicados de WF-001

La ventana es de 24 horas y el disparo es diario a la misma hora, de modo que las ventanas
consecutivas no se solapan y ninguna cita entra en dos. `misfirePolicy: skip` evita que una ejecución
perdida reenvíe una ventana ya cubierta.

## DECISIÓN — El webhook de salida no puede influir en la operación

Tres propiedades, todas por la misma razón:

1. **Asíncrono.** Un n8n lento no alarga la respuesta que espera el usuario.
2. **Sin propagar errores.** Que n8n esté caído no puede impedir que un ADMIN apruebe una cita.
3. **Desactivado por defecto.** Sin `STATUS_WEBHOOK_URL` no se intenta ningún envío.

La URL nunca se registra en el log: puede llevar un identificador secreto en la ruta.

## DECISIÓN — WF-002 dice «no» a lo que no entiende

Respuesta determinista: `200` cuando el evento se procesa, `400` cuando falta un campo mínimo, `422`
cuando el evento es desconocido. Un evento que el workflow no entiende no produce un correo
improvisado.

## HECHO — Validación con ejecución controlada

Tres ejecuciones de WF-002, una por rama, con datos sintéticos. Las dos de rechazo son las que
importan: demuestran que un payload incompleto y un evento desconocido no generan correo.

## PREGUNTA ABIERTA — La cadena completa no se ejecutó

n8n está en la nube y la API en `localhost:8080`. Por eso HU-032 y HU-034 quedan `En validación`: su
CA-01 dice «cuando corre el workflow». Ver `risks-and-open-questions.md`.

## Fuentes

- Evidencia MCP: `docs/evidencia/mcp-n8n.md`
- Riesgos residuales y contenido no confiable: `docs/evidencia/seguridad-contenido-no-confiable.md`
- Contrato: `docs/contratos/agenda-auditoria-e-integraciones-rest.md`
