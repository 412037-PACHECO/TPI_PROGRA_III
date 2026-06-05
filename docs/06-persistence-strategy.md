# 06 - Persistence Strategy

## Objetivo

Persistir suficiente estado para reconstruir una partida completa ante falla, reinicio o reconexión.

Esta estrategia cubre **RF-03 - Gestión del juego y estados** y **RF-05 - Persistencia del estado** desde el diseño técnico. La API REST básica de partidas ya existe para sesión/auditoría, pero WebSocket, frontend y vistas seguras finales quedan para fases posteriores.

## Estado de implementación

- **Implementado hoy**: catálogo local XY1, Deck Builder persistido con JPA/H2, persistencia interna de partidas mediante `GameSessionEntity`, `GameSnapshotEntity`, `GameActionLogEntity`/`GamePersistenceService`, y API REST básica para crear sala, unirse, consultar metadata, listar salas en espera y consultar log persistido.
- **Cobertura de Fase 12**: snapshot JSON completo + metadata consultable + log inmutable append-only, con reconstrucción desde último snapshot.
- **Cobertura de Fase 13 inicial**: endpoints REST de sesión/auditoría sobre la persistencia, sin acciones completas de juego ni vistas seguras finales.
- **Fuera de alcance de este documento/fase**: WebSocket/reconexión, frontend, vistas seguras por jugador y comandos públicos completos de setup/turno/ataque.

## Decisión principal

La partida se persiste con tres piezas separadas:

1. **Metadata consultable de partida**: datos mínimos para listar, filtrar y conocer estado general sin parsear el snapshot.
2. **Snapshot JSON completo de `GameState`**: estado interno íntegro del engine en un punto consistente.
3. **Log inmutable de acciones/eventos**: registros append-only con `sequence` monotónica por partida.

La decisión evita mapear relacionalmente todo el `GameState` porque el engine todavía evoluciona: zonas ocultas, efectos persistentes, selecciones pendientes, condiciones, attachments, evolución y resultados de victoria cambian con frecuencia. Normalizar todo ahora duplicaría el modelo del engine en tablas, aumentaría riesgo de inconsistencias y haría más costosa cada regla nueva.

## Qué se guarda normalizado

- Jugadores.
- Mazos guardados y cartas de mazo.
- Catálogo local de cartas XY1.
- Metadata de partidas: `gameId`, jugadores, estado externo, timestamps, ganador/resultado si existe, turno/fase actual y referencias a snapshot/log mediante `sequence`.
- Snapshots de partida como documentos JSON versionados.
- Logs/eventos de partida como registros inmutables append-only.

No se normaliza cada carta en cada zona, cada attachment, cada condición o cada efecto persistente como tablas independientes en esta fase. Esos datos pertenecen al snapshot completo del `GameState`.

## Qué se guarda como snapshot

El estado interno completo de la partida después de cada acción relevante. El snapshot debe ser un JSON versionado del `GameState` completo, incluyendo información pública y privada necesaria para reconstrucción exacta.

Campos del registro de snapshot implementado/recomendado:

- `id`.
- `gameId`.
- `sequence`: versión incremental del snapshot por partida.
- `snapshotVersion`: versión del formato serializado del engine.
- `actionLogId`: referencia opcional al log que produjo el snapshot.
- `reason`: motivo del snapshot.
- `createdAt`.
- `snapshotJson`: JSON completo del `GameState`.
- `pendingEffectSelectionJson`: selección pendiente interna si la acción quedó esperando elección.
- `checksum` opcional/recomendado para detectar corrupción.

Tradeoff:

- Ventaja: simple, flexible y permite iterar reglas.
- Riesgo: queries complejas sobre estado interno son más difíciles.
- Mitigación: las consultas operativas usan metadata normalizada; las consultas profundas de auditoría parsean snapshots/logs bajo demanda.

## Estado completo requerido para reconstrucción

- Manos de ambos jugadores.
- Mazos con orden exacto.
- Cartas de Premio con orden/ocultamiento.
- Pilas de descarte.
- Pokémon Activo de cada jugador.
- Banca de cada jugador.
- Cartas unidas: energías, herramientas y evoluciones.
- Daño/contadores de cada Pokémon en juego.
- Condiciones especiales activas.
- Estadio activo.
- Flags del turno: energía unida, retiro usado, Partidario usado, Estadio usado, primer turno, etc.
- Fase actual y jugador activo.
- Resultado de partida si está finalizada: ganador, perdedor, motivo, Muerte Súbita pendiente si aplica.
- Selecciones pendientes: reemplazo obligatorio de Activo y selecciones internas de efectos cuando existan.
- Efectos persistentes pendientes.
- IDs y definiciones estructurales necesarias para no depender de consultas a la API externa durante reconstrucción.

El log/evento asociado no reemplaza al snapshot. El snapshot es la fuente rápida de recuperación; el log explica cómo se llegó ahí y permite replay parcial desde el último snapshot.

## Entidades separadas del engine

Las entidades de persistencia no deben entrar al motor. El engine mantiene `GameState`, comandos y eventos como objetos de dominio puros Java, sin JPA ni DTOs REST.

Entidades/persistencia implementadas:

- `GameSessionEntity`: metadata consultable y ciclo externo de partida.
- `GameSnapshotEntity`: JSON completo del `GameState` versionado.
- `GameActionLogEntity`: log append-only de comandos/eventos/resultados con secuencia.

Mapeo de responsabilidades:

| Capa | Responsabilidad |
|---|---|
| Engine puro | Validar comandos, mutar `GameState`, emitir eventos de dominio. |
| Application service de partidas | Orquestar comando, transacción, snapshot, log y metadata. |
| Persistencia | Guardar metadata, snapshot JSON y log append-only. |
| API/WS futuros | Exponer comandos/vistas seguras sin contener reglas de juego. |

## Log inmutable

Cada entrada debe incluir:

- `gameId`.
- `sequence` monotónica por partida, única e incremental.
- Referencia al snapshot posterior si corresponde mediante `actionLogId` en `GameSnapshotEntity`.
- Turno y fase.
- Jugador origen.
- Tipo de comando, acción o evento.
- `commandJson`: payload mínimo del comando/acción.
- `resultJson`: resultado aceptado/rechazado, error controlado o cambio aplicado.
- `eventsJson`: eventos de engine/application asociados.
- Timestamp.
- El log crudo puede contener información privada y no debe usarse como vista de frontend.

El log sirve para auditoría, debugging, reconstrucción parcial y soporte a reconexión. No reemplaza al snapshot completo.

Reglas obligatorias del log:

- Append-only: no se actualizan ni borran entradas para corregir historia.
- `sequence` se asigna dentro de la misma transacción que persiste snapshot/metadata.
- No hay gaps de secuencia para acciones confirmadas.
- Los eventos privados pueden persistirse para reconstrucción, pero deben quedar marcados para no publicarse tal cual por WebSocket/API futura.
- Los reintentos idempotentes deben detectarse por metadata de comando o por control de versión; no deben duplicar efectos.

## Reconstrucción

Estrategia recomendada:

1. Cargar metadata de partida por `gameId`.
2. Cargar el último snapshot consistente.
3. Deserializar `snapshotJson` a `GameState` según `snapshotVersion`.
4. Leer logs posteriores si en el futuro se reduce la frecuencia de snapshots.
5. Reaplicar o verificar esos eventos en orden si existieran entradas posteriores.
6. Validar que la metadata final coincide con el estado reconstruido: estado, ganador, turno y último `sequence`.

En una primera implementación puede guardarse snapshot después de cada acción relevante, dejando el replay de logs como validación/auditoría. Si luego se reduce frecuencia de snapshots, el replay desde último snapshot pasa a ser obligatorio.

## Persistencia después de acciones relevantes

Persistir snapshot + log después de:

- Unirse a partida y crear el `GameState` inicial de dos jugadores.
- Crear sala en espera solo persiste metadata `WAITING`, porque todavía no hay `GameState` válido de dos jugadores.
- Resolver mulligan/setup.
- Robar carta.
- Jugar carta.
- Adjuntar energía.
- Evolucionar.
- Retirar.
- Usar habilidad.
- Declarar/resolver ataque.
- Resolver entre turnos.
- KO/premios/victoria.
- Reemplazo obligatorio de Activo tras KO.
- Resolución de selección pendiente de efecto.
- Finalización por deck-out o Muerte Súbita representada.

## Versionado de snapshots

Cada snapshot debe tener versión incremental para:

- Detectar acciones duplicadas o fuera de orden.
- Resolver reconexión.
- Evitar sobrescritura por concurrencia.

## Limitaciones conocidas

- El snapshot completo contiene zonas ocultas; no puede exponerse directamente a frontend ni WebSocket.
- Los eventos/logs crudos del engine no son contrato público seguro por jugador.
- Para UI/reconexión debe usarse la proyección segura `GET /api/games/{gameId}/view?viewerPlayerId=...` y el log sanitizado `GET /api/games/{gameId}/log?viewerPlayerId=...`.
- Si cambia la estructura de `GameState`, se necesita migración o compatibilidad por `snapshotVersion`.
- Persistir JSON completo reduce fricción del engine, pero dificulta reportes SQL profundos sobre zonas internas.
- La auditoría XY1 sigue siendo incremental: persistir una partida no implica que todos los efectos del set estén soportados.

## API REST básica implementada

Endpoints disponibles:

- `POST /api/games`: crea metadata de sala en espera (`WAITING`) para `playerOneId`.
- `POST /api/games/{gameId}/join`: agrega `playerTwoId`, crea `GameState.CREATED`, persiste snapshot inicial y registra log `GAME_JOINED`.
- `GET /api/games/{gameId}`: consulta metadata normalizada.
- `GET /api/games/waiting`: lista salas en espera.
- `GET /api/games/{gameId}/log?viewerPlayerId=...`: consulta historial público/sanitizado sin JSON crudo.
- `GET /api/games/{gameId}/view?viewerPlayerId=...`: consulta vista segura desde la perspectiva del jugador.

Decisión importante: no se exponen endpoints de acciones de engine hasta cerrar DTOs de mazos/cartas y autorización mínima. Exponer `GameState`, snapshots o logs crudos como contrato final filtraría zonas ocultas.

La operación de join toma lock pesimista sobre `GameSessionEntity` para evitar que dos requests simultáneos unan jugadores distintos a la misma sala `WAITING`.

`viewerPlayerId` selecciona perspectiva, pero no equivale a autenticación real. En una fase con seguridad debe obtenerse de sesión/token y no desde un parámetro controlado por el cliente.

## Vistas seguras y privacidad

La proyección segura separa estado interno de contrato público:

- Mano propia: visible completa.
- Mano rival: solo cantidad.
- Mazo propio/rival: solo cantidad; no se expone orden.
- Premios propios/rivales boca abajo: solo cantidad.
- Campo propio/rival: público, incluyendo Activo, Banca, daño, condiciones, energías y herramienta.
- Descarte propio/rival: público.
- `PendingEffectSelection`: solo el jugador autorizado ve candidatos privados; el rival ve metadata pública de selección pendiente.

WebSocket/reconexión debe emitir estas vistas por jugador, no snapshots crudos.

## Próximo paso recomendado

Evolucionar la **API de partidas** como capa de aplicación sobre esta persistencia:

- enviar comandos de juego,
- preparar contrato de reconexión para WebSocket futuro.

Ese diseño debe hacerse sin mover reglas a controllers ni exponer `GameState` crudo.
