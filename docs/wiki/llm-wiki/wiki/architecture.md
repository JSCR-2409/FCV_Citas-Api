# Arquitectura

Actualizado: 2026-10-04.

## HECHO — Límites entre los dos repositorios

`FCV_Citas-Web` consume `FCV_Citas-Api` directamente por REST. No hay Express ni BFF, conforme a
RF-20 y a `RESTRICCIONES_TECNICAS.md`.

## HECHO — El backend es hexagonal

`RESTRICCIONES_TECNICAS.md` lo exige en su lista de *Definition of Architecture*: «arquitectura
hexagonal: dominio/aplicación independientes de adaptadores». Durante S3, S4 y S5 el backend fueron
paquetes planos con SQL en línea dentro de los controladores, y quedó registrado como PREGUNTA
ABIERTA. Se cumplió al cerrar el proyecto.

```
co.fcv.citas
├── domain/                    ← el centro. Sin Spring, sin JPA, sin HTTP
│   ├── appointment/           Appointment, AppointmentStatus, SlotPlan, SpecialtyKind, ChangeSource
│   └── shared/                DomainRuleViolation
├── application/               ← orquesta casos de uso y declara puertos
│   └── appointment/
│       ├── BookAppointmentUseCase, CancelAppointmentUseCase,
│       │   DecideSpecializedRequestUseCase, CloseAttentionUseCase
│       └── port/out/          AppointmentRepositoryPort, SlotReservationPort,
│                              SpecialtyCatalogPort, StatusHistoryPort,
│                              PendingRescheduleClosurePort
└── adapters/                  ← todo lo que habla con el mundo
    ├── in/rest/               los 17 controladores REST
    ├── in/security/           JwtAuthFilter, IntegrationTokenFilter, RateLimitFilter
    ├── out/persistence/       entidades JPA, repositorios Spring Data y los adaptadores de puerto
    └── out/notification/      StatusChangeNotifier
```

## DECISIÓN — Qué se movió al dominio y qué no

Al dominio fueron las reglas que antes vivían como condiciones dentro de un `WHERE`: qué estados son
terminales, qué cita se puede cancelar, cuántas franjas de 30 minutos necesita una duración, con qué
estado nace una cita según su especialidad, y qué exige un rechazo.

Eso resolvió un problema concreto: la pregunta «¿se puede cancelar esta cita?» estaba escrita **tres
veces**, en tres cláusulas SQL distintas, y nada garantizaba que las tres dijeran lo mismo. Ahora
tiene una sola respuesta y se puede comprobar sin base de datos.

No fueron al dominio las consultas de proyección —bandejas, agenda, auditoría, catálogos—. No
contienen reglas: son lecturas con filtros, y modelarlas como dominio habría sido inventar una capa
sin contenido.

## DECISIÓN — Spring Data JPA para persistir estado; SQL para la concurrencia

La misma lista de restricciones pide «Spring Data JPA para persistencia», y así se persiste el estado
de las citas: `AppointmentEntity` con su `JpaRepository`.

**La excepción es el reclamo de franjas, y es deliberada.** `SlotReservationPort.claim` es un `UPDATE`
condicional que ocupa *solo* las franjas que sigan libres y devuelve cuántas consiguió. Esa guarda
dentro de la propia escritura es lo que sostiene RN-01, porque entre «comprobar que está libre» y
«ocupar» cabe otra reserva. Expresarlo cargando entidades y guardándolas una a una dejaría de ser
atómico y abriría exactamente la ventana que la regla cierra.

Que ese SQL viva en un adaptador es justamente el punto: el caso de uso llama a `claim` y no sabe cómo
está implementado.

## DECISIÓN — La aplicación usa Spring; el dominio no

Los casos de uso llevan `@Service` y `@Transactional`. Es una concesión consciente: la alternativa
—un adaptador que abra la transacción y delegue en una clase sin anotaciones— añade indirección sin
cambiar quién depende de quién.

Lo que la restricción exige es que el **dominio** no dependa de adaptadores, y `co.fcv.citas.domain`
no importa nada de Spring, de JPA, de Jakarta ni de `java.sql`.

## HECHO — El límite está verificado, no solo documentado

`HexagonalBoundariesTest` lee el código fuente y falla si una dependencia va en el sentido prohibido.
Sin ella la restricción sería una intención: nada impide importar un `JdbcTemplate` en el dominio y
seguir compilando.

Comprobado que la prueba **no es vacua**: al introducir un `import org.springframework.stereotype.Component`
en una clase del dominio, falla señalando el fichero y la línea.

## HECHO — El dominio se prueba sin levantar nada

`AppointmentDomainTest` ejerce 25 reglas en **0,03 segundos**. Las clases con `@SpringBootTest` tardan
entre 3 y 7 segundos cada una. Esa diferencia es la medida práctica de la independencia: si el dominio
necesitara un contexto de Spring para probarse, no sería independiente.

## PREGUNTA ABIERTA — La migración no está completa

Lo que sigue en paquetes heredados: `auth` (AuthService, JwtService, PasswordRecoveryService) y
`config` (SecurityConfig). Son servicios de aplicación y configuración de infraestructura, y su
migración es mecánica.

Lo que importa es que **la dirección de las dependencias ya es correcta y está verificada**: nada del
dominio ni de la aplicación depende de un adaptador. Terminar de reubicar esas cinco clases mejora la
simetría del árbol, no el cumplimiento de la restricción.
