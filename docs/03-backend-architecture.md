# 03 - Backend Architecture

## Estilo arquitectónico

Arquitectura por capas con un núcleo de dominio/game-engine desacoplado. Spring Boot, REST, WebSocket, JPA y clientes externos son adaptadores alrededor del motor.

```text
api/rest + realtime/ws
        ↓
application use cases
        ↓
domain / game-engine
        ↓
ports
        ↓
infrastructure persistence / external clients
```

## Paquetes sugeridos

```text
com.tpi.pokemon
├── cards
│   ├── application
│   ├── domain
│   └── infrastructure
├── decks
├── game
│   ├── application
│   ├── engine
│   │   ├── commands
│   │   ├── events
│   │   ├── model
│   │   ├── rules
│   │   ├── resolvers
│   │   └── effects
│   └── infrastructure
├── realtime
├── players
├── shared
└── config
```

## Responsabilidades por capa

### API REST

- Recibir requests.
- Validar forma básica de entrada.
- Mapear DTOs a comandos/casos de uso.
- Devolver respuestas y errores controlados.

No debe calcular daño, validar energía, resolver ataques ni cambiar estado de juego directamente.

### Application

- Orquestar casos de uso.
- Cargar estado desde repositorios.
- Invocar el Game Engine.
- Persistir snapshots/logs.
- Publicar eventos para WebSocket.

No debe duplicar reglas oficiales. Si una regla cambia el resultado del juego, vive en el engine.

### Domain / Game Engine

- Recibir comandos y estado.
- Validar reglas.
- Resolver mutaciones.
- Emitir eventos de dominio.
- Generar o permitir construir vistas seguras.

No debe depender de Spring, JPA, WebSocket, HTTP clients ni pokemontcg.io.

### Infrastructure

- JPA/repositories.
- Migraciones futuras.
- Clientes de API externa.
- Serialización de snapshots.
- Mappers entre entidades y dominio.

No debe tomar decisiones de reglas.

## Reglas de dependencia

- `game.engine` no importa `org.springframework.*`, `jakarta.persistence.*` ni WebSocket.
- Controllers no conocen detalles internos de resolvers.
- WebSocket publica eventos/resultados; no resuelve acciones.
- El catálogo consulta pokemontcg.io solo para importar/cachear cartas, nunca durante una partida.

## Evitar lógica en controllers/services transaccionales

Toda acción de juego debe seguir este flujo:

```text
Controller/WS handler
→ ApplicationService
→ Load Game snapshot
→ GameEngine.apply(command)
→ Persist new snapshot + immutable log
→ Publish events / safe views
```

Si aparece cálculo de daño, validación de fase o resolución de KO fuera del engine, se considera fuga de dominio.
