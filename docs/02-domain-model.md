# 02 - Domain Model

## Entidades principales

La tabla describe el modelo objetivo del TPI. La sección siguiente indica qué partes existen actualmente en código.

| Entidad | Responsabilidad |
|---|---|
| Game | Estado completo de una partida y raíz del agregado del motor. |
| Match | Ciclo de vida externo de la partida: jugadores, estado, persistencia y publicación de eventos. |
| Turn | Jugador activo, número de turno, fase y flags del turno. |
| Player | Identidad del jugador y vínculo con mazos/partidas. |
| Deck | Lista ordenada de cartas de instancia para una partida o plantilla de mazo guardado. |
| CardDefinition | Datos estáticos de una carta: nombre, set, tipos, ataques, habilidades, reglas y metadata. |
| CardInstance | Copia concreta de una carta dentro de una partida; tiene identidad única. |
| PokemonInPlay | Pokémon activo o en banca con daño, condiciones, evolución y cartas unidas. |
| Attack | Definición de coste, daño base y efectos del ataque. |
| Ability | Efecto no considerado ataque, con timing propio. |
| Energy | Carta o tipo de energía disponible para pagar costes. |
| Trainer | Carta de Entrenador: Objeto, Partidario, Estadio o Herramienta. AS TÁCTICO / ACE SPEC queda reservado como subtipo condicional para sets opcionales que lo incluyan; no aplica al alcance base `xy1`. |
| StatusEffect | Dormido, Quemado, Confundido, Paralizado, Envenenado. |
| PrizeCards | Cartas de premio ocultas, ordenadas y asociadas a un jugador. |
| Bench | Hasta 5 Pokémon en banca. |
| ActivePokemon | Único Pokémon activo de un jugador. |
| GameLog | Registro inmutable de acciones, eventos y resultados. |

## Estado de implementación actual

La Fase 9 implementa el modelo interno base, setup/mulligan, motor de turnos con acciones MAIN, ataques base, knockout, premios, condiciones básicas de victoria/derrota, condiciones especiales y daño entre turnos bajo `backend/src/main/java/com/tpi/pokemon/game/`.

Incluye:

- Value objects: `GameId`, `PlayerId`, `CardInstanceId`.
- Enums: `GameStatus`, `TurnPhase`, `ZoneType`, `CardSupertype`, `CardSubtype`, `EnergyType`, `PokemonType`.
- Modelo: `GameState`, `PlayerGameState`, `BoardState`, `TurnState`, `CardDefinitionRef`, `CardInstance`, `PokemonInPlay`, `ActivePokemon`, `Bench`, `AttachedCards`, `DeckZone`, `HandZone`, `PrizeCards`, `DiscardPile`, `StadiumInPlay`, `AttackDefinition`, `EnergyProfile`, `Weakness`, `Resistance`.
- Eventos base: `GameCreatedEvent`, `GameStateInitializedEvent`, `CardMovedEvent`, `TurnPhaseChangedEvent`, eventos de setup/mulligan, eventos de turno/acciones MAIN, eventos de ataque/daño y eventos de KO/premios/victoria.
- Comandos base: `GameCommand`, `PlayerCommand`, `CommandResult`.
- Setup: `SetupService`, `DeckShuffler`, `StartingPlayerSelector`, `MulliganBonusDrawPolicy`, `StartSetupCommand`, `ChooseInitialPokemonCommand`, `CompleteSetupCommand`.
- Turnos: `TurnManager`, `TurnActionService` y comandos de inicio/fin de turno, banca, energía, evolución, retiro y Trainer.
- Ataques: `AttackService`, `EnergyCostValidator`, `DamageCalculator`, `DeclareAttackCommand`.
- Knockout/victoria: `KnockoutResolver`, `PrizeResolver`, `PostAttackResolutionService`, `ActivePokemonReplacementResolver`, `VictoryConditionChecker`, `GameFinishResult`.
- Condiciones especiales: `SpecialCondition`, `SpecialConditionSet`, `StatusEffectManager`, `BetweenTurnsService`, `CoinFlipProvider`.

No implementa todavía efectos complejos XY1, habilidades, daño a Banca, flujo jugable completo de Muerte Súbita, endpoints de partida, WebSocket, persistencia de partidas ni frontend.

## Relaciones

- `Game` contiene dos `PlayerGameState`.
- Cada `PlayerGameState` contiene `Deck`, mano, descarte, premios, activo y banca.
- `PokemonInPlay` referencia una o más `CardInstance`: carta base/evoluciones y cartas unidas.
- `CardInstance` referencia una `CardDefinition`.
- `Turn` pertenece al `Game` y define fase/flags.
- `GameLog` pertenece a `Game/Match` y registra eventos derivados del motor.

## Invariantes implementadas hasta Fase 9

- Una `CardInstance` está en una sola zona lógica: deck, mano, premios, descarte, activo, banca, unida o removida.
- Cada jugador tiene como máximo 1 Pokémon activo.
- La banca tiene máximo 5 Pokémon.
- Las cartas de Premio permiten conteos `0..6` para setup normal, premios restantes durante partida y futura Muerte Súbita.
- El mazo mantiene orden oculto.
- La selección inicial de Activo/Banca solo acepta cartas que estén en mano y sean Pokémon Básicos.
- El setup completo coloca exactamente 6 Premios desde el tope del mazo de cada jugador.
- Solo el jugador actual puede ejecutar acciones MAIN.
- Las acciones MAIN requieren `TurnPhase.MAIN`.
- Una energía manual, un Partidario, un Estadio y un retiro como máximo por turno.
- La evolución estructural conserva cartas unidas y respeta primer turno del jugador, Pokémon recién jugado y evolución previa en el mismo turno.
- Solo el jugador actual puede atacar desde `MAIN` y el engine controla la transición a `ATTACK`.
- El coste de energía de un ataque debe estar cubierto antes de aplicar daño.
- El daño aplicado nunca es negativo y se acumula como contadores de 10.
- Un Pokémon queda noqueado cuando su daño acumulado alcanza o supera su HP.
- Un Pokémon noqueado y sus cartas asociadas salen de mesa y pasan al descarte de su dueño.
- Las cartas de Premio tomadas reducen la cantidad de premios pendientes del jugador y se agregan a su mano.
- Una partida puede finalizar por último Premio tomado.
- Una partida puede finalizar si un jugador queda sin Pokémon en juego.
- Una partida puede finalizar por deck-out cuando un jugador debe robar y no tiene cartas en el mazo.
- Si hay reemplazo de Activo pendiente, el turno no finaliza hasta que el jugador correspondiente promueva desde Banca.
- La simultaneidad/Muerte Súbita se representa explícitamente para evitar declarar un ganador incorrecto.
- Dormido, Confundido y Paralizado son mutuamente excluyentes.
- Quemado y Envenenado pueden coexistir con otras condiciones.
- Un Pokémon Dormido o Paralizado no puede atacar ni retirarse.
- Evolucionar limpia las condiciones especiales del Pokémon evolucionado.
- Retirar limpia condiciones especiales del Pokémon que pasa a Banca.
- El daño entre turnos puede causar KO y debe integrarse con premios, reemplazo de Activo y victoria.

## Reglas de dominio futuras / requeridas por reglamento

- Mano rival, premios ocultos y orden de mazo no se exponen al oponente.
- El flujo completo de Muerte Súbita queda pendiente.
- Efectos complejos que aplican, modifican o previenen condiciones especiales quedan pendientes para fases posteriores.
- Efectos de cartas pueden modificar límites de energía/retiro/Trainer en fases futuras.
- Dormido, Confundido y Paralizado son mutuamente excluyentes.
- Quemado y Envenenado pueden coexistir con otras condiciones.
- Al evolucionar o ir a banca se limpian condiciones especiales según reglamento XY1.

## Aggregate Roots sugeridos

- `Game`: raíz del agregado de reglas y estado de partida.
- `Deck`: raíz para construcción/validación de mazos guardados.
- `CardDefinition`: raíz del catálogo cacheado.
- `Player`: raíz de identidad y pertenencia de recursos.

## Value Objects sugeridos

- `GameId`, `MatchId`, `PlayerId`, `CardDefinitionId`, `CardInstanceId`.
- `DamageCounters`, `HitPoints`, `EnergyCost`, `Weakness`, `Resistance`.
- `GameStatus`, `TurnPhase`, `Zone`, `EffectCategory`, `AttackId`.

## Bounded contexts sugeridos

- **Catalog**: importación/cache de cartas XY1.
- **Decks**: creación, validación y persistencia de mazos.
- **Game Engine**: reglas puras de partida.
- **Matches**: ciclo de vida, persistencia, comandos y publicación de eventos.
- **Realtime**: WebSocket y vistas seguras.
- **Players**: jugadores/autenticación si se incorpora.
