# 07 - Realtime WebSocket Strategy

## Objetivo

Sincronizar partida en tiempo real sin delegar reglas al frontend. WebSocket transporta comandos/eventos/vistas; el backend sigue siendo la única fuente de verdad.

## Canales sugeridos

- `/topic/matches/{matchId}/public`: eventos públicos visibles para ambos jugadores.
- `/user/queue/matches/{matchId}`: vista privada del jugador autenticado/conectado.
- `/app/matches/{matchId}/actions`: envío de comandos del cliente al backend.

La estructura exacta puede ajustarse al stack Spring WebSocket elegido.

## Eventos servidor → cliente

- `MATCH_JOINED`
- `SETUP_REQUIRED`
- `TURN_STARTED`
- `CARD_DRAWN`
- `POKEMON_PLAYED`
- `ENERGY_ATTACHED`
- `TRAINER_PLAYED`
- `ATTACK_RESOLVED`
- `DAMAGE_PLACED`
- `SPECIAL_CONDITION_APPLIED`
- `POKEMON_KNOCKED_OUT`
- `PRIZE_TAKEN`
- `TURN_ENDED`
- `GAME_FINISHED`
- `ERROR`

## Comandos cliente → servidor

El cliente envía intención, no mutación final:

- `PLAY_BASIC_POKEMON`
- `ATTACH_ENERGY`
- `EVOLVE_POKEMON`
- `PLAY_TRAINER`
- `USE_ABILITY`
- `RETREAT`
- `DECLARE_ATTACK`
- `END_TURN`

Todo comando debe incluir `matchId`, `playerId/session`, `clientActionId` y `expectedVersion`.

## Reconexión

Al reconectar:

1. Validar jugador y partida.
2. Enviar snapshot/vista segura actual.
3. Enviar eventos faltantes desde última versión conocida si aplica.
4. Rechazar comandos con `expectedVersion` viejo o inválido, salvo idempotencia reconocida.

## Estado visible por jugador

Cada jugador puede ver:

- Su mano completa.
- Su activo, banca, descarte, premios restantes y mazo como conteo.
- Activo/banca/descarte rival públicos.
- Cantidad de cartas en mano rival.
- Cantidad de premios rivales restantes.
- Cantidad de cartas en mazo rival.
- Log público.

No se debe revelar:

- Mano rival.
- Contenido de premios ocultos propios o rivales antes de tomarlos.
- Orden del mazo propio o rival.
- Cartas buscadas/privadas salvo que una regla indique revelación.

## Acciones duplicadas o fuera de orden

- Usar `clientActionId` para idempotencia.
- Usar `expectedVersion` para control optimista.
- Si el comando ya fue aplicado, devolver resultado existente.
- Si la versión no coincide, rechazar y enviar estado actual.
- Si el jugador no corresponde o la fase cambió, devolver error de regla controlado.

## Riesgos

- Filtrar estado interno completo por comodidad.
- Resolver reglas en el handler WebSocket.
- No persistir antes de publicar eventos.
- Reintentos del cliente que duplican acciones sin idempotencia.
