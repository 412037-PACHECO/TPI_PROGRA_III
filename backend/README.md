# Pokémon TCG Backend

Backend base del TPI Pokémon TCG.

## Alcance actual: Fase 7 - Ataques base

El backend ya cuenta con capacidad de importar/cachear localmente cartas `xy1` desde `pokemontcg.io` v2, Deck Builder, modelo interno de partida, setup/mulligan inicial y motor de turnos/acciones MAIN. La Fase 7 agrega resolución base de ataques del Pokémon Activo: validación de energía, daño base, debilidad, resistencia, contadores de daño y finalización automática de turno.

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
- `PrizeCards` permite `0` para estado no inicializado, `6` para setup normal y `1` para futura Muerte Súbita.
- `CardDefinitionRef` conserva clasificación mínima (`CardSupertype` y `CardSubtype`) para validar Pokémon Básico durante setup sin consultar infraestructura externa.
- `CardDefinitionRef` también conserva metadata estructural opcional (`evolvesFrom`, `retreatCost`) para evolución/retiro sin interpretar texto natural.
- `PokemonInPlay` mantiene pila de evolución, cartas unidas y turnos de entrada/evolución.
- `GameState` puede mantener Estadio activo global como estructura, sin aplicar efectos continuos todavía.
- `CardDefinitionRef` puede conservar ataques, HP, tipos, debilidades, resistencias y perfil de energía estructural para el motor de ataque.
- `PokemonInPlay` conserva contadores de daño acumulados; Fase 7 no resuelve KO aunque el daño alcance el HP.

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
- Los demás inicios de turno roban 1 carta; si el mazo está vacío se emite `DeckOutLossDetectedEvent` y el estado pasa a `FINISHED` como marcador provisional.
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
- Deck-out se marca sin modelo completo de ganador/victoria; eso queda para fases de victoria/derrota.

## Ataques base

La Fase 7 implementa ataques del Pokémon Activo como resolución estructural del engine.

Componentes principales:

- `AttackService`: valida y resuelve declaración de ataque desde `MAIN` con transición interna a `ATTACK`.
- `EnergyCostValidator`: valida coste específico e incoloro sin consumir energías.
- `DamageCalculator`: aplica daño base, debilidad y resistencia.
- Modelo mínimo: `AttackDefinition`, `EnergyType`, `PokemonType`, `Weakness`, `Resistance`, `EnergyProfile`.

Reglas implementadas:

- Solo ataca el jugador actual.
- El ataque se declara desde `TurnPhase.MAIN`; el engine transiciona internamente a `ATTACK` y luego finaliza turno.
- El jugador inicial no puede atacar en su primer turno.
- Solo ataca el Pokémon Activo.
- El ataque debe existir en la definición del Pokémon Activo.
- Debe haber un Pokémon Activo rival.
- Energía específica requiere símbolo del tipo correspondiente.
- Coste `COLORLESS` se paga con cualquier símbolo de energía.
- Los costes específicos se cubren antes que los incoloros.
- Se aplica daño base, luego debilidad, luego resistencia, con mínimo 0.
- El daño se convierte a contadores de 10 y se acumula en el Activo rival.
- Atacar finaliza automáticamente el turno usando `TurnManager`.

Limitaciones honestas de Fase 7:

- No hay knockout, descarte por KO, toma de premios ni victoria/derrota.
- No se resuelven efectos textuales de ataques.
- No se resuelven condiciones especiales como Confundido, Dormido o Paralizado.
- Energías especiales solo cuentan si tienen `EnergyProfile` explícito; no hay efectos dinámicos.

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

- Partida jugable.
- Knockout y premios durante partida.
- Condiciones especiales.
- WebSocket/realtime.
- Frontend.
- Efectos ejecutables.
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
