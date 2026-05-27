# Pokémon TCG Backend

Backend base del TPI Pokémon TCG.

## Alcance actual: Fase 2 - Catálogo/cache local XY1

Esta fase agrega un catálogo local de cartas `xy1` importado desde `pokemontcg.io` v2. El backend guarda una copia fiel de los datos relevantes para consultas locales, sin interpretar texto natural como lógica ejecutable.

Incluye:

- Proyecto Maven bajo `backend/`.
- Spring Boot 3.x.
- Clase principal de arranque.
- Configuración H2 local/dev en `application-local.yml`.
- Test de carga de contexto.
- Módulo `cards` separado en `api`, `application`, `domain` e `infrastructure`.
- Importador de cartas `xy1` desde `pokemontcg.io`.
- Búsqueda local paginada por `setId` y/o nombre parcial.

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

- Reglas oficiales del juego.
- Motor de juego.
- WebSocket/realtime.
- Frontend.
- Deck Builder.
- Efectos ejecutables.
- Validaciones de mazo.
- Regla ACE SPEC obligatoria para `xy1`.

Los campos complejos de cartas (`attacks`, `abilities`, `rules`, `weaknesses`, etc.) se guardan como JSON texto para conservar fidelidad de catálogo sin sobre-modelar ni parsear reglas.

## Dependencias actuales

- `spring-boot-starter-web`: deja la app lista para exponer APIs REST futuras.
- `spring-boot-starter-data-jpa`: persistencia del catálogo local.
- `h2`: base local/dev y tests.
- `spring-boot-starter-test`: permite validar el contexto de Spring desde el inicio.

No se incluye `spring-boot-starter-validation` porque los endpoints actuales usan parámetros simples y no hay DTOs de entrada complejos.

## Comandos

Desde `backend/`:

```bash
mvn test
```

## Convención arquitectónica

La estructura parte de paquetes por módulo de negocio. En `cards` se usan subpaquetes `api`, `application`, `domain` e `infrastructure` porque ya hay controller, servicio, entidad/repository y cliente externo reales.

La lógica oficial del juego no debe vivir en controllers, handlers WebSocket ni servicios de aplicación. Cuando se implemente, debe quedar concentrada en el módulo/motor de juego.
