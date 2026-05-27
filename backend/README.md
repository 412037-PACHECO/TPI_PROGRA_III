# Pokémon TCG Backend

Backend base del TPI Pokémon TCG.

## Alcance actual: Fase 1

Esta fase crea una aplicación Spring Boot mínima y compilable para Java 21, sin implementar reglas de juego ni infraestructura prematura.

Incluye:

- Proyecto Maven bajo `backend/`.
- Spring Boot 3.x.
- Clase principal de arranque.
- Configuración básica en `application.yml`.
- Test de carga de contexto.
- Paquetes base del backend:
  - `cards`
  - `decks`
  - `game`
  - `realtime`
  - `players`
  - `shared`
  - `config`

## Qué NO incluye todavía

- Reglas oficiales del juego.
- Motor de juego.
- WebSocket/realtime.
- Frontend.
- Integración con `pokemontcg.io`.
- Deck Builder.
- Entidades JPA.
- Base de datos.

La base de datos, PostgreSQL/MySQL y las dependencias de persistencia se agregarán en una fase posterior, cuando se implemente catálogo/persistencia. Evitar configurarlas ahora reduce ruido y errores de arranque innecesarios.

## Dependencias actuales

- `spring-boot-starter-web`: deja la app lista para exponer APIs REST futuras.
- `spring-boot-starter-test`: permite validar el contexto de Spring desde el inicio.

No se incluye `spring-boot-starter-validation` porque todavía no hay DTOs ni entrada de usuario para validar.

## Comandos

Desde `backend/`:

```bash
mvn test
```

## Convención arquitectónica inicial

La estructura parte de paquetes por módulo de negocio. Los subpaquetes `domain`, `application`, `infrastructure` o `api` se crearán recién cuando haya clases reales que los justifiquen.

La lógica oficial del juego no debe vivir en controllers, handlers WebSocket ni servicios de aplicación. Cuando se implemente, debe quedar concentrada en el módulo/motor de juego.
