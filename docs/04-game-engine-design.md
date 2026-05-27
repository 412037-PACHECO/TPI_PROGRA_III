# 04 - Game Engine Design

## Principio de diseño

El Game Engine es una máquina de estados determinística de dominio. Recibe un estado y un comando, valida reglas, muta el estado, emite eventos y no conoce infraestructura.

```text
GameCommand
→ CommandValidator
→ RuleResolver / EffectResolver
→ StateMutation
→ DomainEvents
→ SafePlayerViews
```

## Estados de partida

- `WAITING`: partida creada, esperando segundo jugador.
- `SETUP`: mulligan, selección de activo/banca inicial, premios y primer jugador.
- `ACTIVE`: partida en curso.
- `FINISHED`: partida finalizada con ganador o resultado definido.

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

## Flujo de turno

1. `DRAW`: robar carta o perder si el mazo está vacío al inicio del turno.
2. `MAIN`: ejecutar comandos opcionales válidos.
3. `ATTACK`: declarar ataque si permitido.
4. Resolver ataque y finalizar turno.
5. `BETWEEN_TURNS`: procesar condiciones/efectos.
6. Cambiar jugador activo y resetear flags.

## Flujo de ataque

1. Validar jugador, fase y Pokémon activo.
2. Validar que puede atacar: no Dormido/Paralizado, restricciones de primer turno, efectos activos.
3. Validar coste de energía.
4. Si está Confundido, lanzar moneda: cruz cancela ataque, aplica 3 contadores al atacante y termina turno.
5. Resolver selecciones/targets.
6. Ejecutar requisitos previos del ataque.
7. Aplicar efectos que modifican o cancelan el ataque.
8. Calcular daño base.
9. Aplicar modificadores del atacante.
10. Aplicar Debilidad x2.
11. Aplicar Resistencia -20, mínimo 0.
12. Aplicar modificadores del defensor.
13. Colocar contadores de daño.
14. Aplicar efectos posteriores.
15. Resolver knockouts y victoria.

## Flujo de knockout

1. Detectar Pokémon con daño acumulado >= HP.
2. Descartar Pokémon y cartas unidas.
3. Otorgar premios al rival: 1 normal, 2 si Pokémon-EX.
4. Si el jugador toma el último premio, evaluar victoria.
5. Si el dueño del Pokémon activo no puede promover desde banca, pierde.
6. Resolver simultaneidades según reglamento: si ambos ganan simultáneamente, muerte súbita salvo que uno gane por más condiciones.

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

## Comandos de juego sugeridos

- `JoinGame`, `ChooseStartingActive`, `ChooseStartingBench`, `ResolveMulligan`.
- `DrawForTurn`, `PlayBasicPokemon`, `EvolvePokemon`, `AttachEnergy`.
- `PlayTrainer`, `UseAbility`, `Retreat`, `DeclareAttack`, `EndTurn`.

## Validadores y resolutores

- Validadores: estado, turno, fase, zona, target, coste, restricciones, efectos activos.
- Resolutores: setup, mulligan, turnos, ataque, daño, condiciones, knockout, premios y victoria.
