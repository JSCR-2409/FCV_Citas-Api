---
id: HU-007
tipo: historia-de-usuario
titulo: "Consultar y actualizar perfil"
estado: Completada
epica: "[[EP-002-identidad-sesion-y-perfil]]"
esfuerzo: Medio
sprint_sugerido: "Incremento II — Acceso y datos maestros"
dependencias: ["[[HU-004-iniciar-sesion-jwt]]"]
relacionadas: ["[[HU-008-gestionar-afiliacion]]"]
---

# HU-007 — Consultar y actualizar perfil

## Historia de usuario
**COMO** USER autenticado **QUIERO** consultar y modificar mis datos permitidos **PARA** mantener mi información de contacto vigente.

## Alcance
- Consulta y actualización de atributos de perfil que se aprueben por contrato, con ownership del USER.

## Fuera de alcance
- Editar roles, perfiles de terceros, datos clínicos o atributos no permitidos.

## Reglas de negocio
- Autorización por ownership; validación server-side; campos permitidos pendientes de concretar en contrato.

## Dependencias y relaciones
- Épica: [[EP-002-identidad-sesion-y-perfil]]
- Dependencias: [[HU-004-iniciar-sesion-jwt]]
- Relacionada: [[HU-008-gestionar-afiliacion]]

## Esfuerzo
**Nivel:** Medio. Requiere límites claros de edición y consistencia con identidad.

## Tareas de desarrollo
- [ ] **T-01 — Aprobar matriz de campos editables.** Dificultad: Medio. Registrar los no definidos como decisión.
- [ ] **T-02 — Implementar consulta/actualización con ownership y validación.** Dificultad: Medio. Separar caso de uso y adaptadores.
- [ ] **T-03 — Integrar pantalla de perfil y pruebas de acceso.** Dificultad: Medio. Mostrar estados de éxito/error.

## Criterios de aceptación
### CA-01 — Consulta propia
Dado un USER autenticado, cuando consulta su perfil, entonces ve únicamente su información autorizada.
### CA-02 — Actualización válida
Dado un cambio válido en campo permitido, cuando lo guarda, entonces se persiste y se refleja en una consulta posterior.
### CA-03 — Protección de límites
Dado un campo no permitido o un perfil ajeno, cuando se intenta modificar, entonces backend lo rechaza sin alterar datos.

## Definition of Done
- [ ] CA-01 a CA-03 están probados en aplicación/REST y UI aplicable.
- [ ] La matriz de campos editables está aprobada o la HU conserva el impedimento documentado.
- [ ] Si cambia esquema, existe migración Flyway y contrato actualizado en ambos repositorios.
- [ ] La trazabilidad Scrum está actualizada.

## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumplido | ProfileAndMyAppointmentsTest.hu007_ca01_* | Matriz aprobada: names, surnames y phone |
| CA-02 | Cumplido | hu007_ca02_anAllowedFieldIsPersisted... | Matriz aprobada: names, surnames y phone |
| CA-03 | Cumplido | hu007_ca03_* (3) | Matriz aprobada: names, surnames y phone |
| DoD | Cumplido | 6 pruebas + PATCH /me en la UI del paciente | 4/4 |

## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-02 — S4: CA verificados con pruebas automatizadas y UI conectada; estado `Completada`.

## Notas y decisiones

- ~~PREGUNTA ABIERTA: lista exacta de datos permitidos para actualización.~~
  **Resuelta el 2026-10-02 (S4).**

- **DECISIÓN — matriz de campos editables.** Editables por su titular: `names`, `surnames` y `phone`.
  Fuera: el documento y el email, porque identifican la cuenta y son clave de unicidad y de inicio de
  sesión; el estado `active` y los roles, porque son decisiones administrativas. El resto del perfil
  es de solo lectura.

- La protección no es una lista de rechazo sino de admisión: el controlador lee exactamente esos tres
  campos del cuerpo, de modo que un campo no permitido no se ignora *después* de leerse, sino que no
  se lee. Verificado en `hu007_ca03_identityFieldsCannotBeChangedThroughTheProfile`.

- El identificador del usuario sale del token, nunca del cuerpo ni de la ruta, así que el ownership no
  depende de lo que envíe el cliente. Verificado en `hu007_ca03_theUpdateCannotReachAnotherAccount`.
