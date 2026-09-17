---
id: HU-013
tipo: historia-de-usuario
titulo: "Crear profesional"
estado: Pendiente de aprobación
epica: "[[EP-003-catalogos-y-gestion-de-profesionales]]"
esfuerzo: Alto
sprint_sugerido: "Incremento II — Acceso y datos maestros"
dependencias: ["[[HU-004-iniciar-sesion-jwt]]", "[[HU-009-consultar-catalogos-fijos]]"]
relacionadas: ["[[HU-014-asignar-especialidades-profesional]]", "[[HU-015-asignar-sedes-y-estado-profesional]]"]
---

# HU-013 — Crear profesional

## Historia de usuario
**COMO** ADMIN **QUIERO** crear un usuario PROFESSIONAL con código y matrícula ficticia **PARA** habilitar la configuración de su oferta de citas.

## Alcance
- Crear cuenta PROFESSIONAL y registrar código/matrícula sintéticos.

## Fuera de alcance
- Autoregistro profesional, datos o matrículas reales, asignación implícita de especialidad/sede.

## Reglas de negocio
- Solo ADMIN crea PROFESSIONAL; profesional es usuario especializado; datos de laboratorio sintéticos.

## Dependencias y relaciones
- Épica: [[EP-003-catalogos-y-gestion-de-profesionales]]
- Dependencias: [[HU-004-iniciar-sesion-jwt]], [[HU-009-consultar-catalogos-fijos]]
- Relacionadas: [[HU-014-asignar-especialidades-profesional]], [[HU-015-asignar-sedes-y-estado-profesional]]

## Esfuerzo
**Nivel:** Alto. Combina identidad, rol especializado, seguridad y datos únicos.

## Tareas de desarrollo
- [ ] **T-01 — Definir atributos y reglas de unicidad del profesional.** Dificultad: Medio. No asumir formato de matrícula.
- [ ] **T-02 — Implementar creación ADMIN y persistencia especializada.** Dificultad: Alto. Incluir migración necesaria.
- [ ] **T-03 — Integrar formulario y pruebas de RBAC/datos sintéticos.** Dificultad: Medio. No exponer secretos iniciales.

## Criterios de aceptación
### CA-01 — Creación restringida
Dado un ADMIN, cuando registra datos válidos del profesional, entonces se crea un usuario con rol PROFESSIONAL y sus identificadores ficticios.
### CA-02 — No autoregistro
Dado visitante, USER o PROFESSIONAL, cuando intenta crear un profesional, entonces backend lo rechaza.
### CA-03 — Datos de laboratorio
Dado el profesional creado, cuando se revisan sus datos, entonces son sintéticos y no corresponden a información privada real.

## Definition of Done
- [ ] CA-01 a CA-03 tienen pruebas RBAC, unicidad y REST/UI aplicable.
- [ ] El diseño modela al profesional como usuario especializado en 3FN y tiene Flyway si aplica.
- [ ] El contrato no expone credenciales ni obliga a una asignación no aprobada.
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
- PREGUNTA ABIERTA: formato/unicidad precisa de código profesional y matrícula.
