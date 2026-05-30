# Pokémon TCG Backend

Backend base del TPI Pokémon TCG.

## Alcance actual: Fase 11 - Auditoría y mapeo progresivo XY1

El backend ya cuenta con capacidad de importar/cachear localmente cartas `xy1` desde `pokemontcg.io` v2, Deck Builder, modelo interno de partida, setup/mulligan inicial, motor de turnos/acciones MAIN, ataques base, knockout, premios, condiciones básicas de victoria/derrota, condiciones especiales y motor extensible de efectos. La Fase 11 agrega auditoría progresiva de cartas reales XY1 y un primer catálogo explícito de mappings hacia `EffectDefinition`, sin intentar cubrir todo XY1 de golpe y sin parser automático de texto natural.

Incluye:

- Proyecto Maven bajo `backend/`.
- Spring Boot 3.x.
- Clase principal de arranque.
- Configuración H2 local/dev en `application-local.yml`.
- Test de carga de contexto.
- Módulo `cards` separado en `api`, `application`, `domain` e `infrastructure`.
- Importador de cartas `xy1` desde `pokemontcg.io`.
- Búsqueda local paginada por `setId` y/o nombre parcial.
- Módulo `decks` separado en `api`, `application` y `domain`.
- CRUD de mazos por `ownerName` simple, sin relación compleja con `Player`.
- Edición de cartas del mazo usando exclusivamente el catálogo local.
- Validación explícita de mazos XY1.
- Módulo `game` con modelo interno de Game State, value objects, enums, eventos y comandos base.
- Componentes de setup/mulligan para barajar, robar mano inicial, resolver mulligans, seleccionar Activo/Banca inicial, colocar Premios y definir jugador inicial.
- Motor de turnos para inicio de turno, DRAW/MAIN, finalización de turno y acciones MAIN estructurales.
- Motor de ataques base para validar ataque, validar coste de energía, calcular daño y aplicar contadores al Activo rival.
- Resolución de knockout cuando el daño acumulado alcanza o supera el HP.
- Descarte del Pokémon noqueado, pila de evolución y cartas unidas.
- Toma de premios por KO: 1 premio normal, 2 premios para Pokémon-EX.
- Victoria por último Premio, por rival sin Pokémon en juego y por deck-out.
- Reemplazo obligatorio de Activo desde Banca cuando la partida continúa tras un KO.
- Representación explícita de simultaneidad/Muerte Súbita pendiente, sin jugar todavía Muerte Súbita completa.
- Modelo de condiciones especiales: Dormido, Quemado, Confundido, Paralizado y Envenenado.
- Restricciones de ataque/retiro por condición y chequeos con moneda inyectable.
- Daño entre turnos por Envenenado/Quemado e integración con KO/premios/victoria.
- Limpieza de condiciones al evolucionar y al retirar a Banca.
- Motor de efectos basado en definiciones estructuradas, registry y handlers genéricos.
- Integración de efectos simples posteriores al daño dentro de ataques.
- Auditoría XY1 progresiva con estados/categorías explícitas.
- Mappings representativos de cartas reales XY1 a `EffectDefinition` mediante `Xy1EffectCatalog`.
- Herramienta interna para generar reporte de auditoría XY1 desde catálogo local cacheado, sin exponer endpoint público.

## Modelo Game State

La Fase 4 prepara el dominio del Game Engine sin implementar todavía una partida jugable.

Paquetes principales:

- `com.tpi.pokemon.game.domain.value`: IDs de dominio (`GameId`, `PlayerId`, `CardInstanceId`).
- `com.tpi.pokemon.game.domain.enums`: estado de partida, fases y zonas.
- `com.tpi.pokemon.game.domain.model`: `GameState`, estado por jugador, tablero, zonas, cartas instancia y turno.
- `com.tpi.pokemon.game.engine.event`: eventos de dominio base.
- `com.tpi.pokemon.game.engine.command`: contratos mínimos de comandos/resultados.

Decisiones de dominio:

- `CardDefinitionRef` representa una referencia estable a la carta de catálogo.
- `CardInstance` representa una copia concreta dentro de una partida, con identidad propia.
- `GameState` no usa entidades JPA ni DTOs REST.
- El estado interno no es una vista segura para frontend; podrá contener mano, premios y mazo de ambos jugadores.
- `PrizeCards` permite conteos de `0` a `6` para soportar premios restantes durante partida.
- `CardDefinitionRef` conserva clasificación mínima (`CardSupertype` y `CardSubtype`) para validar Pokémon Básico durante setup sin consultar infraestructura externa.
- `CardDefinitionRef` también conserva metadata estructural opcional (`evolvesFrom`, `retreatCost`) para evolución/retiro sin interpretar texto natural.
- `PokemonInPlay` mantiene pila de evolución, cartas unidas y turnos de entrada/evolución.
- `GameState` puede mantener Estadio activo global como estructura, sin aplicar efectos continuos todavía.
- `CardDefinitionRef` puede conservar ataques, HP, tipos, debilidades, resistencias y perfil de energía estructural para el motor de ataque.
- `PokemonInPlay` conserva contadores de daño acumulados; Fase 8 resuelve KO cuando el daño alcanza o supera el HP.
- `PokemonInPlay` conserva condiciones especiales como parte del estado en juego.

Invariantes protegidas en el modelo:

- Una partida base requiere dos jugadores distintos.
- Cada jugador tiene zonas propias.
- Una misma `CardInstanceId` no puede repetirse dentro de una zona ni entre zonas de un mismo jugador.
- La banca tiene máximo 5 Pokémon.
- Solo hay un wrapper de Pokémon Activo por jugador; puede estar vacío antes del setup.
- `TurnState.notStarted()` inicializa número de turno `0`, fase `NOT_STARTED` y flags de turno en `false`.

## Setup y Mulligan

La Fase 5 implementa el flujo inicial oficial como lógica de engine, no como endpoint REST.

Componentes principales:

- `SetupService`: orquesta inicio de setup, elección inicial y cierre de setup.
- `DeckShuffler`: abstracción inyectable para barajar mazos de forma testeable.
- `StartingPlayerSelector`: abstracción inyectable para determinar quién comienza.
- `MulliganBonusDrawPolicy`: policy inyectable para decidir cuántas cartas extra roba un jugador por mulligans del rival, entre `0` y el total permitido.

Flujo implementado:

1. Validar que cada jugador reciba un mazo de 60 `CardInstance` propias y con al menos 1 Pokémon Básico.
2. Barajar cada mazo mediante `DeckShuffler`.
3. Robar 7 cartas iniciales.
4. Si una mano no tiene Pokémon Básico, registrar mulligan con cartas reveladas conceptualmente, volver a barajar y robar 7 hasta encontrar Básico.
5. Aplicar robo bonus por mulligans del rival según `MulliganBonusDrawPolicy`.
6. Elegir 1 Pokémon Básico desde la mano como Activo inicial.
7. Elegir hasta 5 Pokémon Básicos desde la mano para la Banca inicial.
8. Colocar 6 cartas de Premio desde el tope del mazo.
9. Determinar jugador inicial y dejar el juego en `ACTIVE` con `TurnState.preparedForFirstTurn(...)`.

Decisión de fase: al completar setup la partida queda preparada para Fase 6, pero no se ejecuta robo obligatorio de turno, acciones de turno, ataques ni efectos.

## Turnos y acciones MAIN

La Fase 6 implementa estructura de turno y acciones principales, pero todavía no resuelve ataques ni consecuencias de combate.

Componentes principales:

- `TurnManager`: inicia turnos, resuelve DRAW obligatorio o salto de robo del primer jugador, entra a MAIN y finaliza turno.
- `TurnActionService`: ejecuta acciones MAIN estructurales.
- Commands: `StartTurnCommand`, `EndTurnCommand`, `PutBasicPokemonOnBenchCommand`, `AttachEnergyCommand`, `EvolvePokemonCommand`, `RetreatActivePokemonCommand`, `PlayTrainerCommand`.

Reglas implementadas:

- El primer jugador no roba en su primer turno.
- Los demás inicios de turno roban 1 carta; si el mazo está vacío se emite `DeckOutLossDetectedEvent`, se registra victoria del oponente y el estado pasa a `FINISHED`.
- Solo el jugador actual puede actuar.
- Las acciones MAIN solo se permiten en `TurnPhase.MAIN`.
- Bajar Pokémon Básico a Banca desde la mano, respetando máximo 5.
- Unir 1 Energía por turno a un Pokémon propio en juego.
- Evolucionar estructuralmente con `evolvesFrom`, bloqueando primer turno del jugador, Pokémon recién jugados y doble evolución en el mismo turno.
- Retirar estructuralmente una vez por turno si el coste de retirada es conocido y se descartan energías suficientes.
- Jugar Entrenadores estructurales: Item sin límite, Supporter máximo 1, Stadium máximo 1 y Tool única por Pokémon.
- Finalizar turno cambia al oponente, deja fase `NOT_STARTED` y resetea flags para el siguiente inicio de turno.

Limitaciones honestas de Fase 6:

- Las cartas Trainer, Stadium, Tool y Energías Especiales no aplican efectos textuales todavía.
- El retiro no contempla modificadores de coste, condiciones especiales ni energías que cuenten doble.
- Deck-out ya se integra como condición de derrota/victoria; no hay todavía persistencia ni vista pública del resultado.

## Ataques base

La Fase 7 implementa ataques del Pokémon Activo como resolución estructural del engine.

Componentes principales:

- `AttackService`: valida y resuelve declaración de ataque desde `MAIN` con transición interna a `ATTACK`.
- `EnergyCostValidator`: valida coste específico e incoloro sin consumir energías.
- `DamageCalculator`: aplica daño base, debilidad y resistencia.
- Modelo mínimo: `AttackDefinition`, `EnergyType`, `PokemonType`, `Weakness`, `Resistance`, `EnergyProfile`.

Reglas implementadas:

- Solo ataca el jugador actual.
- El ataque se declara desde `TurnPhase.MAIN`; el engine transiciona internamente a `ATTACK` y luego resuelve daño, KO/premios/victoria y fin de turno o reemplazo pendiente.
- El jugador inicial no puede atacar en su primer turno.
- Solo ataca el Pokémon Activo.
- El ataque debe existir en la definición del Pokémon Activo.
- Debe haber un Pokémon Activo rival.
- Energía específica requiere símbolo del tipo correspondiente.
- Coste `COLORLESS` se paga con cualquier símbolo de energía.
- Los costes específicos se cubren antes que los incoloros.
- Se aplica daño base, luego debilidad, luego resistencia, con mínimo 0.
- El daño se convierte a contadores de 10 y se acumula en el Activo rival.
- Atacar finaliza automáticamente el turno usando `TurnManager` solo si no hay victoria ni reemplazo de Activo pendiente.

Limitaciones honestas propias de Fase 7 antes de Fase 8:

- En Fase 7 no había knockout, descarte por KO, toma de premios ni victoria/derrota; Fase 8 lo incorpora en una sección separada.
- No se resuelven efectos textuales de ataques.
- No se resuelven condiciones especiales como Confundido, Dormido o Paralizado.
- Energías especiales solo cuentan si tienen `EnergyProfile` explícito; no hay efectos dinámicos.

## Knockout, premios y victoria

La Fase 8 integra las consecuencias principales del daño dentro del Game Engine puro Java.

Componentes principales:

- `KnockoutResolver`: detecta KO del Activo defensor y mueve Pokémon, evolución y cartas unidas al descarte del dueño.
- `PrizeResolver`: toma premios del jugador que provoca el KO y los agrega a su mano.
- `VictoryConditionChecker`: evalúa victoria por premios, rival sin Pokémon en juego, deck-out y estructura de simultaneidad.
- `PostAttackResolutionService`: orquesta la resolución post-daño desde `AttackService`.
- `ActivePokemonReplacementResolver`: promueve un Pokémon de Banca cuando el defensor debe reemplazar Activo.

Reglas implementadas:

- Un Pokémon queda Fuera de Combate cuando `damageCounters * 10 >= hp`.
- El Pokémon noqueado, su pila de evolución y sus cartas unidas pasan al descarte del dueño.
- El jugador que provoca el KO toma 1 Premio por Pokémon normal y 2 por Pokémon-EX.
- Si quedan menos Premios que los requeridos, se toman los restantes.
- Tomar el último Premio finaliza la partida con victoria.
- Si el dueño del Pokémon noqueado queda sin Activo y sin Banca, pierde.
- Si tiene Banca y la partida no terminó, queda `PendingActiveReplacement` hasta elegir nuevo Activo.
- Deck-out en `TurnManager.startTurn` finaliza la partida con victoria del oponente.
- La simultaneidad/Muerte Súbita queda representada mediante `GameFinishResult` sin jugar el flujo completo.

Limitaciones honestas de Fase 8:

- Solo se resuelve KO del Activo defensor causado por ataque base; no hay daño a Banca todavía.
- En Fase 8 no se implementaban condiciones especiales ni daño entre turnos; Fase 9 lo incorpora en una sección separada.
- No se implementan efectos complejos que modifiquen premios, daño, descarte o victoria.
- No se juega Muerte Súbita completa; solo queda representada como resultado pendiente.
- No hay endpoints REST de juego, WebSocket, frontend ni persistencia de partida.

## Condiciones especiales y daño entre turnos

La Fase 9 incorpora condiciones especiales al estado de `PokemonInPlay` y las procesa dentro del Game Engine puro Java, sin interpretar texto libre de cartas.

Componentes principales:

- `SpecialCondition` y `SpecialConditionSet`: modelo de condiciones especiales.
- `StatusEffectManager`: aplica, remueve, limpia y consulta condiciones.
- `BetweenTurnsService`: procesa daño/chequeos entre turnos y coordina KO/premios/victoria.
- `CoinFlipProvider`: abstracción inyectable para chequeos de moneda testeables.

Reglas implementadas:

- Dormido impide atacar y retirarse; entre turnos lanza moneda y se cura con cara.
- Paralizado impide atacar y retirarse; se limpia entre turnos.
- Confundido permite intentar atacar; con cruz el ataque falla y el atacante recibe 3 contadores de daño.
- Envenenado aplica 1 contador de daño entre turnos.
- Quemado lanza moneda entre turnos; con cruz aplica 2 contadores de daño.
- Dormido, Confundido y Paralizado son mutuamente excluyentes; la condición más reciente reemplaza a la anterior.
- Quemado y Envenenado coexisten con cualquier otra condición y entre sí.
- Evolucionar limpia todas las condiciones especiales del Pokémon evolucionado.
- Retirar limpia todas las condiciones del Pokémon que se mueve a Banca.
- El daño por condición puede provocar KO, premios, reemplazo de Activo y victoria.

Limitaciones honestas de Fase 9:

- No se implementan efectos complejos de cartas XY1 que apliquen/modifiquen condiciones automáticamente.
- No se implementan habilidades.
- No se implementa daño a Banca.
- No se implementan modificadores por Herramientas, Estadios, Energías Especiales o texto complejo.
- No hay endpoints REST de juego, WebSocket, frontend ni persistencia de partida.

## Motor de efectos de cartas

La Fase 10 implementa la base para ejecutar efectos reales de cartas sin convertir el engine en condicionales por `cardId` ni en un parser de texto natural.

Componentes principales:

- `EffectDefinition`: representación estructurada de un efecto auditable.
- `EffectExecutionContext`: partida, jugador, fuente, targets, timing y servicios deterministas disponibles.
- `EffectHandler`: valida y ejecuta una categoría de efecto sobre `GameState`.
- `EffectRegistry`: resuelve handlers genéricos y excepciones custom explícitas.
- `EffectExecutionService`: ejecuta efectos simples o compuestos en orden.
- Matriz `docs/11-xy1-audit-matrix.md`: registra qué cartas/efectos XY1 están clasificados, implementados y testeados.

Handlers genéricos disponibles hasta Fase 11C e infraestructura de Fase 11D:

- `DealDamageEffectHandler`.
- `HealDamageEffectHandler`.
- `ApplySpecialConditionEffectHandler`.
- `DrawCardsEffectHandler`.
- `DiscardAttachedEnergyEffectHandler`.
- `SearchDeckEffectHandler`.
- `ShuffleDeckEffectHandler`.
- `DiscardCardsEffectHandler`.
- `AttachEnergyEffectHandler`.
- `MoveEnergyEffectHandler`.
- `SwitchActiveEffectHandler`.
- `PlaceDamageCountersEffectHandler`.
- `CoinFlipEffectHandler`.
- `CompositeEffectHandler`.
- `CardEffectDefinition` + `EffectSourceCollector` para efectos continuos declarados por carta.
- `ModifierResolver` para modificadores de daño, prevención de daño, coste de retirada y prevención de condiciones especiales.

Reglas de diseño:

- No se parsea automáticamente el texto natural de cartas como mecanismo de ejecución.
- Los campos textuales del catálogo sirven como evidencia/auditoría, no como lógica ejecutable.
- Primero se implementan handlers genéricos reutilizables; los handlers custom se justifican carta por carta.
- Una carta XY1 se considera cubierta solo si su fila de auditoría indica implementación y tests.
- El robo por efecto toma hasta la cantidad disponible del mazo y no dispara deck-out; deck-out sigue reservado al robo obligatorio de turno.
- La curación se expresa en puntos de daño y no baja de 0.

Limitaciones honestas de Fase 10:

- No todas las cartas XY1 están mapeadas.
- No hay ejecución completa de habilidades, Trainers, Estadios, Herramientas ni Energías Especiales.
- No hay persistencia de partidas, WebSocket ni vistas seguras por jugador.

### Fase 11C - Primera tanda de handlers faltantes

La Fase 11C desbloquea categorías genéricas frecuentes detectadas por la auditoría XY1, sin mapear todavía las 146 cartas completas.

Categorías desbloqueadas:

- `SEARCH_DECK`: búsqueda estructurada en mazo con filtro simple y selección explícita.
- `SHUFFLE_DECK`: barajado por efecto mediante `DeckShuffler` inyectable.
- `DISCARD_CARDS`: descarte desde mano o mazo.
- `ATTACH_ENERGY`: adjuntar Energía por efecto desde mano, descarte o mazo.
- `MOVE_ENERGY`: mover Energía entre Pokémon propios.
- `SWITCH_ACTIVE`: cambiar Activo por Pokémon de Banca cuando la selección ya está resuelta.
- `PLACE_DAMAGE_COUNTERS`: colocar contadores directamente, distinto del daño de ataque.

Limitaciones:

- La selección pendiente queda modelada en `EffectResult` y eventos internos; no hay contrato público de API/frontend todavía.
- `PlaceDamageCountersEffectHandler` integra KO/premios/victoria para Activo; distribución compleja y KOs de Banca quedan para fases futuras.
- No se implementan habilidades pasivas/reactivas/continuas, prevent damage completo, modificadores globales de daño ni modificadores de retreat cost.
- No hay WebSocket, frontend, persistencia de partida ni endpoints REST de juego.

### Fase 11D - Infraestructura de abilities y modificadores

La Fase 11D incorpora infraestructura pura Java para efectos persistentes sin implementar todavía cobertura completa de cartas:

- `CardDefinitionRef` puede declarar `CardEffectDefinition` para abilities, Tools, Stadiums y otros efectos de carta.
- `EffectSourceCollector` descubre fuentes activas desde Pokémon en juego, Herramientas adjuntas y Estadio activo.
- `DamageCalculator` tiene cálculo contextual con modificadores antes/después de debilidad/resistencia y prevención.
- `TurnActionService` aplica modificadores de coste de retirada.
- `StatusEffectManager` permite prevención contextual de condiciones especiales.
- Eventos nuevos registran modificación/prevención para auditoría del motor.

Limitación honesta: esta infraestructura no marca cartas XY1 como soportadas por sí sola. Cada carta requiere mapping explícito, validación de timing y tests.

## Auditoría y mapping XY1

La Fase 11 incorpora trazabilidad concreta entre cartas reales XY1 y efectos estructurados del engine.

Componentes principales:

- `Xy1EffectCatalog`: catálogo puro Java de mappings explícitos para el set `xy1`.
- `AttackEffectMapping`: vínculo entre `cardId`, ataque real y `EffectDefinition`.
- `TrainerEffectMapping`: vínculo entre `cardId`, carta Trainer real, efectos al jugarla y efectos continuos de Tool/Stadium cuando aplica.
- `AbilityEffectMapping`: vínculo entre `cardId`, habilidad Pokémon real y efectos continuos/preventivos cuando aplica.
- `EnergyEffectMapping`: vínculo entre `cardId`, carta Energy real, perfil de energía estructural y gaps de Energía Especial cuando aplica.
- `Xy1AuditEntry`: metadata de auditoría para documentación y tests.
- `Xy1AuditStatus`: estados como `DATA_IMPORTED`, `EFFECT_CLASSIFIED`, `EFFECT_MAPPED`, `FULLY_TESTED`, `REQUIRES_CUSTOM_HANDLER` y `NOT_IMPLEMENTED_YET`.
- `Xy1EffectCategory`: categorías como `DAMAGE_ONLY`, `DAMAGE_PLUS_STATUS`, `DAMAGE_PLUS_HEAL`, `DRAW_CARDS`, `DISCARD_ENERGY`, `ABILITY_PASSIVE` y `CONTINUOUS_EFFECT`.

Mappings representativos implementados/testeados:

- `xy1-1 Venusaur-EX / Poison Powder`: aplica `POISONED` al Activo defensor.
- `xy1-1 Venusaur-EX / Jungle Hammer`: cura 30 al Activo atacante.
- `xy1-10 Pansage / Vine Whip`: daño base puro, sin efecto adicional.
- `xy1-10 Pansage / Leech Seed`: cura 10 al Activo atacante.
- `xy1-16 Spewpa / Stun Spore`: moneda; si sale cara, aplica `PARALYZED`.
- `xy1-68 Sableye / Filch`: roba 1 carta.
- `xy1-68 Sableye / Rip Claw`: moneda; si sale cara, descarta 1 Energía del Activo defensor.

Fase 11E.2 agrega mapeo progresivo de Trainers XY1 usando handlers existentes o infraestructura continua:

- `xy1-119 Hard Charm`: Tool continua que reduce 20 daño recibido después de Debilidad/Resistencia.
- `xy1-121 Muscle Band`: Tool continua que suma 20 daño antes de Debilidad/Resistencia.
- `xy1-123 Professor's Letter`: búsqueda/reveal/shuffle estructural, pendiente de contrato público de selección/privacidad.
- `xy1-125 Roller Skates`: moneda; con cara roba 3 cartas.
- `xy1-129 Team Flare Grunt`: descarta una Energía del Activo rival.

Fase 11E.3 agrega mapeo progresivo de habilidades Pokémon XY1 usando la infraestructura continua de Fase 11D:

- `xy1-114 Furfrou / Fur Coat`: habilidad continua que reduce 20 daño recibido después de Debilidad/Resistencia.
- `xy1-95 Slurpuff / Sweet Veil`: prevención parcial de nuevas condiciones especiales para Pokémon propios con Energía Fairy unida; falta remover condiciones existentes.
- `xy1-14 Chesnaught / Spiky Shield`: queda documentada como gap porque requiere resolver reactivo al recibir daño de ataque y colocar contadores en el atacante.

Fase 11E.4 audita y mapea Energías XY1:

- `xy1-132` a `xy1-140`: Energías Básicas sin efecto textual; se tratan como datos estructurales mediante `EnergyProfile.basic(...)`.
- `xy1-130 Double Colorless Energy`: Energía Especial mapeada como `EnergyProfile.of(COLORLESS, COLORLESS)` para pagar dos símbolos incoloros.
- `xy1-131 Rainbow Energy`: queda pendiente para soporte completo porque requiere provisión dinámica de cualquier tipo como una sola Energía a la vez y un trigger al adjuntarse desde mano que pone 1 contador de daño.

Esto no implica soporte completo de Trainers, Stadiums, Tools ni efectos continuos. La ejecución pública de Trainers que requieren selección desde mazo/mano, reveal, shuffle o privacidad de zonas ocultas sigue limitada porque no hay endpoints REST de partida, WebSocket, frontend ni vistas seguras por jugador.

Gaps documentados:

- `xy1-123 Professor's Letter`: búsqueda en mazo + reveal + shuffle.
- `xy1-127 Shauna`: mezclar mano en mazo + robar.
- Trainers con búsqueda desde mazo: falta contrato público de selección, reveal, shuffle y privacidad.
- Trainers que mezclan mano/mazo y luego roban: requieren composición completa y eventos seguros.
- Stadium/Tool con efectos continuos condicionados por Energía unida: requieren mapping carta por carta, timing y tests.
- `xy1-14 Chesnaught / Spiky Shield`: habilidad pasiva/reactiva.
- `xy1-95 Slurpuff / Sweet Veil`: falta cleanup continuo de condiciones existentes para soporte completo.
- `xy1-131 Rainbow Energy`: falta energía dinámica one-at-a-time y trigger `on attach from hand`.

Reglas de diseño de Fase 11:

- `Xy1EffectCatalog.isCompleteAudit()` devuelve `false`; no se afirma cobertura completa de las 146 cartas.
- Una carta/ataque sin mapping devuelve `List.of()` y no rompe daño base.
- La ausencia de mapping no equivale a “sin efecto real”; se consulta la matriz de auditoría.
- No se parsea texto natural automáticamente.
- No se agregan WebSocket, frontend, persistencia de partida ni endpoints REST de juego.

### Auditoría completa desde cache local

La subfase `feature/xy1-full-audit` agrega soporte interno para auditar las 146 cartas cuando el catálogo local esté importado:

- `Xy1AuditService.generateReportFromLocalCache()` lee `setId=xy1` desde `CardRepository`.
- `Xy1AuditReportGenerator` produce conteos, entradas por carta y gaps.
- `Xy1CardClassifier` clasifica ataques, habilidades y reglas desde JSON/texto cacheado.

En este entorno no había `backend/data` local disponible, por lo que no se afirma que el reporte completo de 146 cartas haya sido ejecutado acá. Para generar el reporte real primero importá:

```http
POST /api/cards/import/xy1
```

Luego verificá que haya 146 cartas cacheadas antes de cerrar la auditoría completa.

## Endpoints de catálogo

### Importar/cachear XY1

```http
POST /api/cards/import/xy1
```

Status esperado: `202 Accepted`.

Respuesta:

```json
{
  "received": 146,
  "created": 146,
  "updated": 0,
  "skipped": 0,
  "errors": 0
}
```

### Listar/buscar cartas cacheadas

```http
GET /api/cards?setId=xy1&name=venus&page=0&size=20
```

- `setId`: opcional.
- `name`: opcional, búsqueda parcial case-insensitive.
- `page`: opcional, default `0`.
- `size`: opcional, default `20`, máximo `100`.

Respuesta paginada: `content`, `page`, `size`, `totalElements`, `totalPages`.

### Obtener carta por ID oficial

```http
GET /api/cards/{cardId}
```

Ejemplo:

```http
GET /api/cards/xy1-1
```

## Endpoints Deck Builder

El Deck Builder depende del catálogo local. Antes de agregar cartas, importá/cacheá `xy1` con `POST /api/cards/import/xy1`. No consume `pokemontcg.io` directamente desde decks.

### Crear mazo vacío

```http
POST /api/decks
Content-Type: application/json

{
  "name": "Mi mazo XY1",
  "ownerName": "Ash"
}
```

### Listar mazos por owner

```http
GET /api/decks?owner=Ash
```

### Obtener detalle

```http
GET /api/decks/{deckId}
```

El detalle incluye `cardId`, `name`, `supertype`, `subtypes`, `imageSmall` y `quantity` de cada carta, enriquecidos desde el catálogo local.

### Editar datos básicos

```http
PUT /api/decks/{deckId}
Content-Type: application/json

{
  "name": "Mi mazo editado",
  "ownerName": "Ash"
}
```

No se fuerza unicidad de nombre por owner en esta fase.

### Eliminar mazo

```http
DELETE /api/decks/{deckId}
```

Status esperado: `204 No Content`.

### Agregar o actualizar carta

```http
PUT /api/decks/{deckId}/cards/xy1-1
Content-Type: application/json

{
  "quantity": 4
}
```

- `quantity < 0`: error `400`.
- `quantity = 0`: remueve la carta del mazo.
- Carta inexistente en catálogo local: error `404`.
- Carta de set distinto de `xy1`: error `400`.

### Quitar carta

```http
DELETE /api/decks/{deckId}/cards/xy1-1
```

### Validar mazo

```http
GET /api/decks/{deckId}/validation
```

Respuesta mínima:

```json
{
  "valid": false,
  "totalCards": 12,
  "errors": [
    {
      "code": "DECK_SIZE_NOT_60",
      "cardId": null,
      "cardName": null,
      "message": "El mazo debe tener exactamente 60 cartas"
    }
  ],
  "warnings": [
    "AS TÁCTICO / ACE SPEC no se valida como regla obligatoria para mazos xy1."
  ]
}
```

Reglas XY1 implementadas solo en el endpoint explícito de validación:

- Exactamente 60 cartas para `valid=true`.
- Máximo 4 copias por nombre de carta.
- Energía Básica sin límite. Se detecta por `supertype` Energy/Energía y subtype Basic/Básica; adicionalmente se contemplan nombres simples de energías básicas.
- Al menos 1 Pokémon Básico.
- Errores posibles: `DECK_SIZE_NOT_60`, `TOO_MANY_COPIES`, `NO_BASIC_POKEMON`, `CARD_NOT_FOUND`, `NON_XY1_CARD`.
- AS TÁCTICO / ACE SPEC NO es validación obligatoria para `xy1`.

Los mazos incompletos pueden guardarse y editarse. Las reglas de mazo completo no bloquean el CRUD.

## Base de datos local/dev

Se usa H2 bajo el perfil `local` como base de desarrollo inicial para mantener la fase académica liviana y migrable a PostgreSQL/MySQL más adelante.

El perfil `local` queda como perfil por defecto en `application.yml` para facilitar la ejecución local. La configuración concreta de H2 vive en `application-local.yml`.

- URL local: `jdbc:h2:file:./data/pokemon-tcg;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`
- Consola H2: `http://localhost:8080/h2-console`
- `ddl-auto: update` para evolucionar el esquema durante esta fase.

No se agrega PostgreSQL/MySQL todavía para evitar infraestructura prematura.

## Configuración pokemontcg.io

```yaml
pokemon-tcg:
  api:
    base-url: https://api.pokemontcg.io/v2
    api-key: ${POKEMON_TCG_API_KEY:}
```

La API key es opcional y se lee desde variable de entorno. No hardcodear secretos.

## Qué NO incluye todavía

- Partida completa jugable de punta a punta vía API pública.
- Daño a Banca.
- Flujo completo de Muerte Súbita.
- Efectos complejos de ataques, habilidades, Trainers, Estadios, Herramientas o Energías Especiales.
- Mapeo completo de todos los efectos XY1.
- Parseo automático de texto natural de cartas.
- Endpoints REST de juego.
- Persistencia de partidas.
- WebSocket/realtime.
- Frontend.
- Regla ACE SPEC obligatoria para `xy1`.

Los campos complejos de cartas (`attacks`, `abilities`, `rules`, `weaknesses`, etc.) se guardan como JSON texto para conservar fidelidad de catálogo sin sobre-modelar ni parsear reglas.

## Dependencias actuales

- `spring-boot-starter-web`: deja la app lista para exponer APIs REST futuras.
- `spring-boot-starter-data-jpa`: persistencia del catálogo local.
- `h2`: base local/dev y tests.
- `spring-boot-starter-test`: permite validar el contexto de Spring desde el inicio.

No se incluye `spring-boot-starter-validation`; los DTOs de Deck Builder se validan manualmente con mensajes simples para evitar agregar dependencias en esta fase.

## Comandos

Desde `backend/`:

```bash
mvn test
```

## Convención arquitectónica

La estructura parte de paquetes por módulo de negocio. En `cards` se usan subpaquetes `api`, `application`, `domain` e `infrastructure` porque ya hay controller, servicio, entidad/repository y cliente externo reales.

La lógica oficial del juego no debe vivir en controllers, handlers WebSocket ni servicios de aplicación. Cuando se implemente, debe quedar concentrada en el módulo/motor de juego. El módulo `game` actual es intencionalmente puro Java y no depende de Spring ni JPA.
