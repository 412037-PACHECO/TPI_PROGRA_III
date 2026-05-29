# 04 - Game Engine Design

## Principio de diseño

El Game Engine es una máquina de estados determinística de dominio. Recibe un estado y un comando, valida reglas, muta el estado, emite eventos y no conoce infraestructura.

Estado actual: la Fase 8 implementa el modelo interno base de `GameState`, setup/mulligan, estructura de turno, acciones MAIN, ataques base, knockout, premios y condiciones básicas de victoria/derrota. Condiciones especiales, efectos complejos, WebSocket, endpoints de partida, persistencia y frontend quedan fuera del alcance actual.

```text
GameCommand
→ CommandValidator
→ RuleResolver / EffectResolver
→ StateMutation
→ DomainEvents
→ SafePlayerViews
```

## Estados de partida

Estados implementados en el modelo actual:

- `CREATED`: estado base creado con dos jugadores, sin setup resuelto.
- `SETUP`: setup iniciado, manos/mulligans resueltos, esperando selección inicial o cierre.
- `ACTIVE`: setup completado o turno en curso.
- `FINISHED`: estado terminal del modelo. En Fase 8 incluye `GameFinishResult` para victoria por premios, rival sin Pokémon en juego o deck-out.

Estados requeridos por diseño funcional/futuro:

- `WAITING`: partida creada, esperando segundo jugador.
- `SETUP`: mulligan, selección de activo/banca inicial, premios y primer jugador.
- `ACTIVE`: partida en curso.
- `FINISHED`: partida finalizada con ganador o resultado definido.

Nota: `WAITING` pertenece más al ciclo externo de match/lobby. El `GameState` interno de Fase 4 se crea con dos jugadores y por eso arranca en `CREATED`.

## Fases de turno

- `DRAW`: robo obligatorio, excepto primer turno del jugador inicial.
- `MAIN`: acciones opcionales en cualquier orden válido.
- `ATTACK`: declaración/resolución de ataque; atacar finaliza el turno.
- `BETWEEN_TURNS`: resolución de condiciones y efectos entre turnos.

## Flujo de setup

1. Validar mazos válidos de 60 cartas y al menos 1 Pokémon Básico.
2. Barajar mazos.
3. Robar 7 cartas.
4. Resolver mulligan hasta que ambos tengan Básico.
5. Permitir selección de Pokémon Activo boca abajo.
6. Permitir hasta 5 Básicos en banca boca abajo.
7. Tomar 6 premios boca abajo.
8. Determinar primer jugador por moneda.
9. Revelar Pokémon y pasar a `ACTIVE`.

Implementación Fase 5:

- El barajado se realiza mediante `DeckShuffler`, no con randomness directa dentro del motor.
- El jugador inicial se determina con `StartingPlayerSelector`, inyectable y determinista en tests.
- El bonus por mulligan rival se modela con `MulliganBonusDrawPolicy`; puede decidir de `0` hasta la cantidad de mulligans del oponente.
- El mulligan registra evento con la mano revelada conceptualmente (`CardInstanceId`), pero no existe todavía vista pública/privada.
- La implementación no modela todavía visibilidad boca abajo/revelada; queda para futuras vistas seguras por jugador.
- Al completar setup se usa `GameStatus.ACTIVE` y `TurnState.preparedForFirstTurn(startingPlayer)`: se conoce quién empieza, pero no se ejecuta inicio real de turno ni robo obligatorio.

## Flujo de turno

Implementado hasta Fase 8:

1. `NOT_STARTED`: turno preparado para el jugador actual.
2. `DRAW`: `TurnManager.startTurn` incrementa turno y resuelve robo obligatorio.
3. Excepción: el jugador inicial no roba en su primer turno.
4. Si debe robar y el mazo está vacío, se emite `DeckOutLossDetectedEvent`, se registra victoria del oponente y el estado pasa a `FINISHED`.
5. `MAIN`: ejecutar acciones principales estructurales válidas.
6. `ATTACK`: declarar/resolver ataque.
7. Aplicar daño.
8. Evaluar knockout, premios y victoria.
9. Si la partida continúa sin reemplazo pendiente, `TurnManager.endTurn` cambia al oponente, resetea flags y deja `NOT_STARTED`.

Pendiente para fases posteriores:

1. `BETWEEN_TURNS`: procesar condiciones/efectos.
2. Efectos complejos de cartas.

## Acciones MAIN implementadas en Fase 6

- Bajar Pokémon Básico desde la mano a la Banca, máximo 5.
- Unir una Energía por turno a un Pokémon propio.
- Evolucionar respetando `evolvesFrom`, primer turno del jugador, Pokémon recién jugado y una evolución por Pokémon por turno.
- Retirar Activo una vez por turno si el coste de retirada es conocido y se pagan energías adjuntas suficientes.
- Jugar Trainer estructuralmente:
  - Item: sin límite, mano → descarte.
  - Supporter: máximo 1 por turno, mano → descarte.
  - Stadium: máximo 1 por turno, queda como estadio activo global y reemplaza el anterior.
  - Tool: se une a un Pokémon propio, máximo 1 herramienta por Pokémon.

Limitaciones Fase 6:

- No se aplican efectos textuales de Trainer, Stadium, Tool, habilidades ni Energías Especiales.
- No se modelan condiciones especiales ni restricciones que impidan retiro.
- No se aplican modificadores de coste de retirada.

## Flujo de ataque

Implementado en Fase 7:

1. Validar `GameStatus.ACTIVE`.
2. Validar que ataca el jugador actual.
3. Validar que el ataque se declara desde `MAIN`; el engine pasa internamente a `ATTACK`.
4. Validar que el jugador inicial no ataque en su primer turno.
5. Validar Pokémon Activo atacante y Pokémon Activo defensor.
6. Validar que el ataque seleccionado exista en el Pokémon Activo atacante.
7. Validar coste de energía sin consumir energías: primero símbolos específicos, después `COLORLESS`.
8. Calcular daño base.
9. Aplicar Debilidad si el tipo del atacante coincide con la debilidad del defensor.
10. Aplicar Resistencia si corresponde, con mínimo 0.
11. Colocar contadores de daño sobre el Activo rival.
12. Emitir eventos de ataque/daño.
13. Resolver KO/premios/victoria si corresponde.
14. Finalizar el turno, dejar reemplazo pendiente o finalizar partida según resultado.

Pendiente para fases posteriores:

- Efectos textuales de ataques.
- Condiciones especiales como Confundido/Dormido/Paralizado.
- Modificadores de daño por habilidades, herramientas, estadios o efectos persistentes.
- Daño a Banca, condiciones especiales, daño entre turnos y efectos complejos.

## Flujo de knockout

Implementado en Fase 8:

1. Detectar el Activo defensor con daño acumulado `>= HP`.
2. Descartar pila de evolución y cartas unidas al descarte del dueño.
3. Otorgar premios al jugador que causó el KO: 1 normal, 2 si Pokémon-EX.
4. Si el jugador toma el último Premio, finalizar partida.
5. Si el dueño del Pokémon activo queda sin Pokémon en juego, finalizar partida.
6. Si el dueño tiene Banca y la partida continúa, dejar `PendingActiveReplacement`.
7. Resolver el reemplazo con `ReplaceActivePokemonCommand` y luego finalizar el turno.
8. Representar simultaneidad/Muerte Súbita con `GameFinishResult` sin jugar el flujo completo.

## Flujo de victoria/derrota

- Victoria por tomar última carta de Premio.
- Victoria por dejar al rival sin Pokémon en juego.
- Victoria por mazo vacío del rival al intentar robar al inicio del turno.
- Muerte Súbita ante victoria simultánea equivalente.

## Condiciones especiales

- Dormido: no puede atacar ni retirarse; entre turnos moneda, cara despierta.
- Quemado: entre turnos moneda; cruz aplica 2 contadores.
- Confundido: al atacar moneda; cruz cancela ataque y aplica 3 contadores al atacante.
- Paralizado: no puede atacar ni retirarse; se limpia al final del turno correspondiente.
- Envenenado: entre turnos aplica 1 contador.

Orden entre turnos: Envenenado → Quemado → Dormido → Paralizado → efectos de habilidades/cartas → KO.

## Eventos de dominio sugeridos

- `GameCreated`, `GameStarted`, `TurnStarted`, `CardDrawn`, `MulliganDeclared`.
- `PokemonPlayed`, `PokemonEvolved`, `EnergyAttached`, `TrainerPlayed`.
- `AttackDeclared`, `DamageCalculated`, `DamagePlaced`.
- `SpecialConditionApplied`, `PokemonKnockedOut`, `PrizeTaken`.
- `TurnEnded`, `VictoryDeclared`, `GameFinished`.

Implementados como estructura base hasta Fase 8:

- `GameCreatedEvent`.
- `GameStateInitializedEvent`.
- `CardMovedEvent`.
- `TurnPhaseChangedEvent`.
- `SetupStartedEvent`.
- `DeckShuffledEvent`.
- `InitialHandDrawnEvent`.
- `MulliganPerformedEvent`.
- `MulliganBonusCardsDrawnEvent`.
- `InitialActivePokemonSelectedEvent`.
- `InitialBenchSelectedEvent`.
- `PrizeCardsSetEvent`.
- `StartingPlayerSelectedEvent`.
- `SetupCompletedEvent`.
- `TurnStartedEvent`.
- `CardDrawnEvent`.
- `CardDrawSkippedEvent`.
- `MainPhaseStartedEvent`.
- `BasicPokemonBenchedEvent`.
- `EnergyAttachedEvent`.
- `PokemonEvolvedEvent`.
- `ActivePokemonRetreatedEvent`.
- `TrainerPlayedEvent`.
- `StadiumReplacedEvent`.
- `TurnEndedEvent`.
- `DeckOutLossDetectedEvent`.
- `AttackDeclaredEvent`.
- `EnergyCostValidatedEvent`.
- `DamageCalculatedEvent`.
- `DamageAppliedEvent`.
- `AttackResolvedEvent`.
- `PokemonKnockedOutEvent`.
- `CardsDiscardedEvent`.
- `PrizeCardsTakenEvent`.
- `ActivePokemonReplacementRequiredEvent`.
- `ActivePokemonReplacedEvent`.
- `VictoryDetectedEvent`.
- `GameFinishedEvent`.
- `SuddenDeathRequiredEvent`.

## Comandos de juego sugeridos

- `JoinGame`, `ChooseStartingActive`, `ChooseStartingBench`, `ResolveMulligan`.
- `DrawForTurn`, `PlayBasicPokemon`, `EvolvePokemon`, `AttachEnergy`.
- `PlayTrainer`, `UseAbility`, `Retreat`, `DeclareAttack`, `EndTurn`.

En Fase 8 existen comandos/modelos específicos de setup, turno/acciones MAIN, declaración de ataque base y reemplazo de Activo post-KO. Los comandos de condiciones especiales y efectos complejos siguen pendientes.

## Reglas de acoplamiento del modelo actual

- `game` no depende de Spring, JPA, controllers, WebSocket ni cliente externo.
- El catálogo se referencia mediante `CardDefinitionRef`; el estado mutable de partida usa `CardInstance`.
- `GameState` es estado interno del motor, no DTO público ni vista segura por jugador.

## Validadores y resolutores

- Implementados: validaciones/resolución de setup, mulligan inicial, turno básico, acciones MAIN estructurales, ataque base, coste de energía, daño base con debilidad/resistencia, knockout, premios, reemplazo de Activo y condiciones básicas de victoria.
- Futuros: condiciones especiales, efectos activos/complejos, daño a Banca, Muerte Súbita jugable, persistencia y vistas seguras.
