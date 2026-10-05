---
tipo: evidencia-s5
estado: Registrado
---

# Invocación MCP contra n8n — S5 y S6

## Cliente y servidor

`GUIA_SESIONES_S2_S6.md` pide distinguir los dos papeles, y en este montaje son:

- **Servidor MCP:** la instancia de n8n (`jasacar.app.n8n.cloud`). Expone herramientas —buscar
  workflows, crear desde código, validar, ejecutar, listar ejecuciones— y es quien **ejecuta** la
  acción.
- **Cliente MCP:** el agente de esta sesión. **Descubre** las herramientas, decide cuál usar y con
  qué argumentos, y recibe el resultado.

La distinción importa por una razón concreta: el servidor no valida la *intención*, solo la forma de
la llamada. Que una herramienta exista no significa que sea correcto invocarla, y esa decisión es
del cliente. Por eso ningún workflow quedó activado.

## Herramientas invocadas y para qué

| Herramienta | Uso real en esta sesión |
|---|---|
| `get_user_preferences` | Primera llamada, antes de crear nada, según la instrucción del servidor |
| `search_workflows` | Inventario previo: la instancia solo tenía un workflow ajeno al proyecto |
| `list_credentials` | Comprobar qué credenciales existen. **Devolvió lista vacía** |
| `get_workflow_sdk_reference` | Leer el lenguaje del SDK antes de escribir código |
| `search_nodes` | Localizar los tipos reales de nodo de Gmail, HTTP Request y Schedule |
| `get_node_types` | Obtener los parámetros exactos de cada nodo, con sus versiones |
| `create_workflow_from_code` | Crear WF-001, WF-002 y WF-003 |
| `archive_workflow` | Retirar el primer intento de WF-001, que tenía dos avisos |
| `get_workflow_details` | Verificar nodos y conexiones de los tres |
| `prepare_workflow_pin_data` | Preparar los datos de la ejecución controlada |
| `test_workflow` | Tres ejecuciones controladas de WF-002 |
| `search_workflow_executions` | Confirmar el resultado de las tres |

## Workflows creados

| Workflow | Id en la instancia | Nodos | Estado |
|---|---|---|---|
| WF-001 Recordatorio de citas próximas | `Q8qFh3h5DUuJl3cd` | 7 | Inactivo |
| WF-002 Notificación por cambio de estado | `zjNkNjCTPFGUJE0J` | 12 | Inactivo |
| WF-003 Resumen operativo diario | `ffrxhi4R8ZqeRzJq` | 6 | Inactivo |

Los tres quedan **inactivos a propósito**. S6 lo dice de forma literal: «No activar un flujo sin
validar salida esperada». Falta la credencial de Gmail, de modo que la salida esperada —un correo—
todavía no se puede validar.

Hay un cuarto workflow archivado, `GuB3xF0OIXV20UR4`: el primer intento de WF-001. Se retiró en lugar
de corregirlo porque tenía dos avisos del servidor, y queda mencionado aquí para que el historial de
la instancia sea legible.

## Dos avisos del servidor que corrigieron un error real

El primer intento de WF-001 devolvió:

```text
PLAIN_GENERIC_AUTH: 'Consultar citas proximas' creates a new httpHeaderAuth credential.
Set genericAuthType to 'httpTemplatedCustomAuth' instead — or credential setup will reject it.

INVALID_PARAMETER: Node "Sticky Note": Field "parameters.content" has wrong type.
Expected string, but got object.
```

Ambos eran errores míos, no del servidor. El segundo es el más instructivo: había escrito la nota
adhesiva con la forma `sticky({ config: { content } })` cuando la firma real es posicional,
`sticky(content, nodes?, options?)`. El workflow se habría creado con una nota vacía y **nadie lo
habría notado hasta abrirlo**, porque una nota no participa en la ejecución.

Es el argumento a favor de leer la referencia del SDK antes de escribir y de mirar los avisos en
lugar de dar por bueno un `201`.

## Ejecución controlada de WF-002

Tres ejecuciones, una por rama, para comprobar que el enrutado y la respuesta son deterministas.
Todas con datos sintéticos.

| Ejecución | Entrada | Rama esperada | Resultado |
|---|---|---|---|
| `1` | `APPOINTMENT_CANCELLED` completo | Correo de cancelación → Trazabilidad → 200 | `success` |
| `2` | `APPOINTMENT_DECIDED` sin `appointmentId` ni `patientEmail` | Responder 400 | `success` |
| `3` | `EVENTO_INVENTADO` con payload válido | Responder 422 | `success` |

`success` en las tres significa que el workflow **terminó según su diseño**, no que enviara un
correo. En la ejecución 1 el nodo Gmail no tenía credencial y, por su `continueRegularOutput`, la
cadena continuó hasta la respuesta. Eso es justo lo que se quería verificar: un fallo de envío deja
traza y responde, en lugar de dejar al backend esperando.

Las ejecuciones 2 y 3 son las que importan más, porque demuestran que **el workflow dice «no»**: un
payload incompleto y un evento desconocido no producen correo.

## Verificación en vivo del webhook de salida

Esta parte **sí se probó de punta a punta en la dirección que la topología permite**. Con
`STATUS_WEBHOOK_URL` apuntando a la URL de producción de WF-002, se canceló una cita real desde la API:

```text
PATCH /api/v1/me/appointments/15/cancel
→ HTTP 200  {"id":15,"status":"CANCELLED"}

WARN  c.f.c.integration.StatusChangeNotifier : El webhook de estados respondió 404 para la cita 15
```

El `404` es la respuesta correcta de n8n para un workflow inactivo, y es justo lo que demuestra lo que
había que demostrar:

| Lo que prueba | Cómo |
|---|---|
| El backend alcanza n8n | Hubo respuesta HTTP, no un fallo de red |
| El fallo no bloquea la operación | La cancelación devolvió `200` antes de que el intento terminara |
| El fallo no se propaga | Quedó en un `WARN`, y la cita se canceló correctamente |
| La URL no se registra | El log lleva el código y el id de la cita, nunca la URL |
| La transición se audita igual | `appointment_status_history` registró `CANCELLED` / `USER` / actor |

Los datos de esa verificación se eliminaron después: 16 usuarios, 8 citas y 4 entradas de historial,
los mismos valores que antes de empezar.

## Lo que esta evidencia no demuestra

- Que llegue un correo. Falta la credencial de Gmail OAuth2, que cada estudiante crea con su propia
  cuenta de Google Cloud.
- Que WF-001 y WF-003 se ejecuten contra datos reales: su nodo HTTP apunta a un marcador de posición
  que hay que sustituir por la URL pública de la API, y n8n no alcanza `localhost`.

Los dos puntos son de despliegue y de credenciales, no de construcción de los workflows.

## Privilegio mínimo de las credenciales

Las tres credenciales que hay que crear, con lo que debe poder cada una:

| Credencial | Alcance mínimo razonable |
|---|---|
| Token de integración (cabecera) | Solo `/api/v1/integrations/**`, solo lectura. No abre nada administrativo |
| Header Auth entrante del webhook | Solo permite entregar eventos a WF-002. No lee nada |
| Gmail OAuth2 | Solo el envío (`gmail.send`). No requiere lectura del buzón |

Ninguna forma parte de los JSON versionados. Comprobado: `grep` de bloques de credenciales sobre los
tres archivos no devuelve nada.
