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

La Fase 6 implementa el modelo interno base, setup/mulligan y motor de turnos con acciones MAIN bajo `backend/src/main/java/com/tpi/pokemon/game/`.

Incluye:

- Value objects: `GameId`, `PlayerId`, `CardInstanceId`.
- Enums: `GameStatus`, `TurnPhase`, `ZoneType`, `CardSupertype`, `CardSubtype`.
- Modelo: `GameState`, `PlayerGameState`, `BoardState`, `TurnState`, `CardDefinitionRef`, `CardInstance`, `PokemonInPlay`, `ActivePokemon`, `Bench`, `AttachedCards`, `DeckZone`, `HandZone`, `PrizeCards`, `DiscardPile`, `StadiumInPlay`.
- Eventos base: `GameCreatedEvent`, `GameStateInitializedEvent`, `CardMovedEvent`, `TurnPhaseChangedEvent`, eventos de setup/mulligan y eventos de turno/acciones MAIN.
- Comandos base: `GameCommand`, `PlayerCommand`, `CommandResult`.
- Setup: `SetupService`, `DeckShuffler`, `StartingPlayerSelector`, `MulliganBonusDrawPolicy`, `StartSetupCommand`, `ChooseInitialPokemonCommand`, `CompleteSetupCommand`.
- Turnos: `TurnManager`, `TurnActionService` y comandos de inicio/fin de turno, banca, energía, evolución, retiro y Trainer.

No implementa todavía ataques, daño, knockout, condiciones especiales, efectos complejos, endpoints de partida, WebSocket ni frontend.

## Relaciones

- `Game` contiene dos `PlayerGameState`.
- Cada `PlayerGameState` contiene `Deck`, mano, descarte, premios, activo y banca.
- `PokemonInPlay` referencia una o más `CardInstance`: carta base/evoluciones y cartas unidas.
- `CardInstance` referencia una `CardDefinition`.
- `Turn` pertenece al `Game` y define fase/flags.
- `GameLog` pertenece a `Game/Match` y registra eventos derivados del motor.

## Invariantes implementadas hasta Fase 6

- Una `CardInstance` está en una sola zona lógica: deck, mano, premios, descarte, activo, banca, unida o removida.
- Cada jugador tiene como máximo 1 Pokémon activo.
- La banca tiene máximo 5 Pokémon.
- Las cartas de Premio permiten `0` en estado no inicializado, `6` en setup normal y `1` para futura Muerte Súbita.
- El mazo mantiene orden oculto.
- La selección inicial de Activo/Banca solo acepta cartas que estén en mano y sean Pokémon Básicos.
- El setup completo coloca exactamente 6 Premios desde el tope del mazo de cada jugador.
- Solo el jugador actual puede ejecutar acciones MAIN.
- Las acciones MAIN requieren `TurnPhase.MAIN`.
- Una energía manual, un Partidario, un Estadio y un retiro como máximo por turno.
- La evolución estructural conserva cartas unidas y respeta primer turno del jugador, Pokémon recién jugado y evolución previa en el mismo turno.

## Reglas de dominio futuras / requeridas por reglamento

- Mano rival, premios ocultos y orden de mazo no se exponen al oponente.
- Ataques, cálculo de daño, knockout, premios durante partida y victoria quedan pendientes.
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
