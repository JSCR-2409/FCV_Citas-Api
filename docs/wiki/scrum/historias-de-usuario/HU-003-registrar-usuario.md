---
id: HU-003
tipo: historia-de-usuario
titulo: "Registrar usuario"
estado: Pendiente de aprobación
epica: "[[EP-002-identidad-sesion-y-perfil]]"
esfuerzo: Medio
sprint_sugerido: "Incremento II — Acceso y datos maestros"
dependencias: ["[[HU-001-modelar-datos-3fn]]", "[[HU-002-documentar-contrato-rest-inicial]]"]
relacionadas: ["[[HU-004-iniciar-sesion-jwt]]"]
---

# HU-003 — Registrar usuario

## Historia de usuario
**COMO** visitante **QUIERO** crear una cuenta USER con mis datos mínimos **PARA** poder usar el sistema de citas ficticio.

## Alcance
- Nombres, apellidos, tipo/número de documento, email, teléfono y contraseña; creación con rol USER.

## Fuera de alcance
- Crear cuentas ADMIN/PROFESSIONAL, datos reales o almacenamiento de contraseña en texto plano.

## Reglas de negocio
- Email y documento únicos; contraseña con hash adaptativo compatible con Spring Security; validación server-side.

## Dependencias y relaciones
- Épica: [[EP-002-identidad-sesion-y-perfil]]
- Dependencias: [[HU-001-modelar-datos-3fn]], [[HU-002-documentar-contrato-rest-inicial]]
- Relacionada: [[HU-004-iniciar-sesion-jwt]]

## Esfuerzo
**Nivel:** Medio. Combina validación, persistencia segura, contrato y experiencia de formulario.

## Tareas de desarrollo
- [ ] **T-01 — Incorporar caso de uso y validaciones de registro.** Dificultad: Medio. Separar dominio, aplicación y adaptadores.
- [ ] **T-02 — Persistir usuario/rol con unicidad y hash adaptativo.** Dificultad: Medio. Incluir migración si cambia esquema.
- [ ] **T-03 — Exponer contrato y formulario acorde al diseño aprobado.** Dificultad: Medio. Verificar errores sin revelar secretos.

## Criterios de aceptación
### CA-01 — Registro válido
Dado un visitante con todos los datos mínimos válidos, cuando confirma el registro, entonces se crea una cuenta con rol USER y resultado seguro observable.
### CA-02 — Unicidad protegida
Dado email o documento ya registrado, cuando se intenta registrar, entonces se rechaza sin crear cuenta duplicada.
### CA-03 — Credenciales protegidas
Dado un registro, cuando se inspecciona persistencia, respuesta y logs aplicables, entonces la contraseña no aparece en texto plano.

## Definition of Done
- [ ] CA-01 a CA-03 están cubiertos por pruebas relevantes de dominio/aplicación/REST y validación UI si existe.
- [ ] Hay migración Flyway y restricción de unicidad coherentes si la HU introduce esquema.
- [ ] El contrato frontend/backend y CORS aplicable están verificados sin BFF.
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
- Los requisitos no fijan política de complejidad de contraseña; no se debe asumir sin aprobación.
