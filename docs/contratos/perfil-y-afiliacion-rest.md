# Contrato REST de perfil y afiliación

Alcance: HU-007, HU-008, HU-010 y HU-011. Versión 1.0.

## Perfil propio — HU-007

* `GET /api/v1/me` devuelve `200` con `id`, `names`, `surnames`, `documentType`, `documentNumber`,
  `email`, `phone`, `active`, `roles` y `role`. Nunca expone el hash de la contraseña.
* `PATCH /api/v1/me` admite exactamente tres campos: `names`, `surnames` y `phone`. Devuelve `200` con
  el perfil actualizado, o `400` si ninguno de los tres trae un valor no vacío.

**Matriz de campos editables (DECISIÓN).** Fuera del alcance editable quedan el documento y el email,
porque identifican la cuenta y son clave de unicidad y de inicio de sesión; y `active` y los roles,
porque son decisiones administrativas.

La protección es por admisión, no por rechazo: el controlador lee únicamente esos tres campos del
cuerpo, de modo que un campo no permitido no se ignora *después* de leerse, sino que no se lee. El
identificador del usuario sale del token, nunca del cuerpo ni de la ruta.

## Afiliación del usuario — HU-008

* `GET /api/v1/me/insurance-affiliation` devuelve `200` con `{"affiliation": null}` cuando no hay
  ninguna registrada, y con el objeto cuando sí. No tener EPS es un estado normal del perfil, no un
  error, de modo que no devuelve `404`.
* `PUT /api/v1/me/insurance-affiliation` recibe `planId` y `membershipNumber`, y opcionalmente
  `epsId`. Devuelve `201` cuando crea la afiliación y `200` cuando reactiva una anterior. Devuelve
  `400` si el plan no existe o no está activo, o si el `epsId` enviado no corresponde al plan.

El objeto de afiliación lleva `id`, `membershipNumber`, `planId`, `planName`, `epsId`, `epsName`,
`regimeId`, `regimeName` y `catalogActive`.

**DECISIÓN — el cliente envía solo `planId`.** El plan ya determina su EPS y su régimen, así que pedir
los tres crearía la posibilidad de una combinación incoherente, que es justo lo que CA-02 prohíbe.
`epsId` se admite pero se verifica.

**DECISIÓN — sin copiar nombres.** La tabla guarda únicamente `plan_id`; la EPS y el régimen se
derivan por `JOIN`. Renombrar una EPS se refleja en las afiliaciones existentes sin migrar nada, que
es lo que exige el requisito 3FN.

**DECISIÓN — una sola afiliación vigente.** RF-04 pide «evitar duplicar EPS, régimen y plan dentro del
usuario». Al guardar una nueva, la anterior pasa a `is_current=false` y queda como histórico.

**DECISIÓN — `catalogActive`.** Si la EPS o el plan se desactivan, la afiliación **no se borra ni se
invalida**: conserva su referente conforme a RF-06, y el campo permite que la UI pida al usuario que
registre una vigente sin bloquearle el resto del portal.

## Catálogo de aseguramiento para elegir — HU-008

* `GET /api/v1/catalogs/eps` devuelve las EPS **activas**, con `id`, `code` y `name`.
* `GET /api/v1/catalogs/eps/{epsId}/plans` devuelve los planes activos de esa EPS, con `id`, `code`,
  `name`, `regimeId` y `regimeName`. El filtro por EPS no es opcional: sin él la lista no serviría
  para elegir, y mezclaría planes de aseguradoras distintas.

## Administración de EPS y planes — HU-010 y HU-011

Todas bajo `/api/v1/admin`, de modo que exigen rol `ADMIN`; cualquier otro actor recibe `403`.

* `GET /api/v1/admin/eps` devuelve **todas**, incluidas las inactivas, con `planCount`. Son justo las
  inactivas las que el ADMIN necesita ver para reactivarlas.
* `POST /api/v1/admin/eps` recibe `code` y `name`. Devuelve `201`, o `409` si el código ya existe, o
  `400` si falta alguno.
* `PATCH /api/v1/admin/eps/{id}` admite `name` y `active`. Devuelve `200`, `404` si no existe o `400`
  si no hay cambios válidos. Desactivar la EPS **desactiva sus planes en cascada**: un plan activo de
  una EPS inactiva sería seleccionable y produciría afiliaciones a una aseguradora que ya no opera.
* `GET /api/v1/admin/eps-plans` admite `epsId`. Devuelve los planes con su EPS y su régimen.
* `POST /api/v1/admin/eps-plans` recibe `epsId`, `regimeId`, `code` y `name`. Devuelve `201`, `409` si
  esa EPS ya tiene un plan con ese código, o `400` si la EPS no existe o está inactiva.
* `PATCH /api/v1/admin/eps-plans/{id}` admite `name`, `regimeId` y `active`. Reactivar un plan cuya
  EPS sigue inactiva devuelve `400`.

**No existe `DELETE` en ninguno de los dos.** RF-06 prohíbe el borrado físico de un catálogo
referenciado por transacciones: la baja es `active=false`, que conserva el significado de las
afiliaciones y citas que ya apuntan a esa EPS o a ese plan.

**El plan no cambia de EPS.** `PATCH` no admite `epsId`: mover un plan reescribiría el significado de
las afiliaciones existentes.

## Evidencia cross-repo

Backend: `ProfileAndMyAppointmentsTest` (6 pruebas de HU-007) e `InsuranceManagementTest` (18).
Frontend: panel «Mis datos y afiliación» en `patient-portal.ts` y panel de convenios en
`admin-portal.ts`.
