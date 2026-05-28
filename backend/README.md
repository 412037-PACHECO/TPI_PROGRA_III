# Pokémon TCG Backend

Backend base del TPI Pokémon TCG.

## Alcance actual: Fase 5 - Setup y Mulligan

El backend ya cuenta con capacidad de importar/cachear localmente cartas `xy1` desde `pokemontcg.io` v2, Deck Builder y modelo interno base de estado de partida. La Fase 5 agrega el flujo inicial de setup/mulligan del Game Engine: puro Java, en memoria, testeable y desacoplado de Spring/JPA/controllers/WebSocket/API externa.

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
- Turnos reales.
- Ataques.
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
