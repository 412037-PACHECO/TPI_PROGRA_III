# 09 - Roadmap

## Fase 1 - Infraestructura base backend

- Objetivo: preparar proyecto Spring Boot o confirmar estructura existente.
- Entregables: `pom.xml`, paquetes base, health endpoint opcional, configuración test básica.
- Dependencias: confirmación del equipo para crear backend.
- Riesgos: crear estructura demasiado grande.
- Criterio de aceptación: proyecto compila y paquetes base existen, sin reglas implementadas.

## Fase 2 - Catálogo y caché de cartas XY1

- Estado: implementada y validada con tests automatizados.
- Objetivo: importar/cachear cartas `xy1` desde pokemontcg.io v2.
- Entregables: modelo catálogo JPA, H2 local/dev, cliente pokemontcg.io, importador idempotente, búsqueda local paginada por `setId` y/o nombre.
- Dependencias: Fase 1, H2 local/dev elegido para fase académica y preparado para migrar.
- Riesgos: datos API no calzan directo con engine; campos complejos se conservan como JSON texto y no deben tratarse como lógica ejecutable.
- Criterio: cartas XY1 disponibles localmente sin consultar API durante partida; importación no duplica por `cardId` oficial.

Endpoints fase 2:

- `POST /api/cards/import/xy1`: importa/cachea `xy1` y devuelve resumen `received/created/updated/skipped/errors`.
- `GET /api/cards?setId=xy1&name=...&page=0&size=20`: lista cartas cacheadas con filtros opcionales.
- `GET /api/cards/{cardId}`: obtiene una carta cacheada por ID oficial.

Fuera de alcance explícito de fase 2: Game Engine, Deck Builder, WebSockets, frontend, efectos ejecutables, validaciones de mazo y ACE SPEC obligatorio para `xy1`.

## Fase 3 - Deck Builder backend

- Estado: implementada en backend.
- Objetivo: CRUD y validación de mazos.
- Entregables: decks, deck cards, endpoints REST y validaciones obligatorias para alcance base `xy1`: exactamente 60 cartas, máximo 4 copias por nombre salvo Energía Básica, y al menos 1 Pokémon Básico.
- Dependencias: catálogo.
- Riesgos: detección de Energía Básica basada en campos de catálogo JSON texto y nombres simples; reglas de nombre Pokémon mal interpretadas; implementar por error AS TÁCTICO / ACE SPEC como validación obligatoria aunque `xy1` no contiene esa mecánica.
- Criterio: mazo válido/inválido detectado con mensajes claros.

Nota: la validación "máximo 1 AS TÁCTICO / ACE SPEC por mazo" no aplica a mazos solo `xy1`. Queda como validación condicional/futura si se incorporan sets opcionales que incluyan cartas ACE SPEC.

Endpoints fase 3:

- `POST /api/decks`: crea mazo vacío.
- `GET /api/decks?owner=...`: lista mazos por `ownerName` case-insensitive.
- `GET /api/decks/{deckId}`: obtiene detalle enriquecido con datos del catálogo local.
- `PUT /api/decks/{deckId}`: edita nombre y `ownerName`.
- `DELETE /api/decks/{deckId}`: elimina mazo.
- `PUT /api/decks/{deckId}/cards/{cardId}`: agrega/actualiza cantidad; `quantity=0` remueve.
- `DELETE /api/decks/{deckId}/cards/{cardId}`: quita carta.
- `GET /api/decks/{deckId}/validation`: valida reglas XY1 sin bloquear guardado de mazos incompletos.

Decisiones fase 3:

- `ownerName` queda como string simple; no hay relación con `Player` todavía.
- No se define unicidad de nombre de mazo por owner.
- Deck Builder solo usa `CardRepository` local; no llama a pokemontcg.io.
- El CRUD permite mazos incompletos; la completitud se evalúa solo en `/validation`.

## Fase 4 - Modelo de Game State

- Estado: implementada como modelo interno puro Java y validada con tests automatizados.
- Objetivo: estado completo en memoria y preparable para serialización futura de snapshots.
- Entregables: `GameState`, `PlayerGameState`, zonas, `CardInstance`, `PokemonInPlay`, `TurnState`, eventos y comandos mínimos.
- Dependencias: catálogo/decks.
- Riesgos: zonas duplicadas o estado incompleto; semántica futura de premios intermedios durante partida deberá definirse cuando existan setup/turnos/KO.
- Criterio: modelo en memoria con invariantes testeadas, sin setup/mulligan, ataques, efectos, WebSocket ni endpoints de juego.

Decisiones fase 4:

- `CardDefinitionRef` separa la definición del catálogo de `CardInstance`, que representa una copia concreta en partida.
- `GameState` queda desacoplado de JPA, Spring, controllers y API externa.
- `PrizeCards` ahora permite conteos `0..6` para cubrir setup normal, premios restantes durante partida y futura Muerte Súbita.
- `TurnState` incluye flags futuros de una vez por turno: energía, Partidario, Estadio y retiro.

## Fase 5 - Setup y mulligan

- Estado: implementada como flujo de engine puro Java, pendiente de ejecución local final de `mvn test` por restricción de entorno.
- Objetivo: preparación oficial.
- Entregables: barajar, robar 7, resolver mulligans, contar bonus por mulligan rival, seleccionar Activo/Banca inicial, colocar premios y determinar jugador inicial.
- Dependencias: Game State.
- Riesgos: timing de mulligan y decisión de UI futura para aceptar/omitir robo bonus opcional.
- Criterio: tests unitarios de mano inicial, mulligan simple/repetido, bonus, selección inicial, premios y estado final consistente.

Decisiones fase 5:

- `DeckShuffler`, `StartingPlayerSelector` y `MulliganBonusDrawPolicy` son abstracciones inyectables para mantener determinismo en tests.
- `CardDefinitionRef` incorpora `CardSupertype`/`CardSubtype` para identificar Pokémon Básico sin consultar JPA ni API externa.
- `MulliganPerformedEvent` registra IDs de la mano revelada conceptualmente.
- Setup completo deja `GameStatus.ACTIVE` y `TurnState.preparedForFirstTurn(jugadorInicial)`, sin iniciar turno ni robar carta de turno.

## Fase 6 - Turnos y acciones básicas

- Estado: implementada como motor puro Java, pendiente de ejecución local final de `mvn test` por restricción de entorno.
- Objetivo: estructura de turno DRAW/MAIN y acciones principales previas a ataque.
- Entregables: `TurnManager`, `TurnActionService`, flags de turno, robo obligatorio, deck-out provisional, banca, energía, evolución, retiro y Trainers estructurales.
- Dependencias: setup.
- Riesgos: metadata incompleta para `evolvesFrom`/`retreatCost`; retiro no contempla modificadores ni condiciones; Trainers no aplican efectos.
- Criterio: tests unitarios de fase/turno/jugador, robo, deck-out, flags y acciones MAIN.

Decisiones fase 6:

- `TurnManager.startTurn` resuelve DRAW y pasa a MAIN porque todavía no hay decisiones durante DRAW.
- El primer jugador saltea su primer robo; el segundo jugador roba normalmente.
- Deck-out fue integrado en Fase 8 como derrota/victoria con resultado de partida.
- `PokemonInPlay` usa pila de evolución y conserva attachments al evolucionar.
- `GameState` mantiene estadio activo global; efectos de estadio quedan pendientes.

## Fase 7 - Ataques base

- Estado: implementada como motor puro Java y validada localmente con Maven antes de Fase 8.
- Objetivo: declarar ataque, validar energía y resolver daño base contra el Activo rival.
- Entregables: `AttackService`, `EnergyCostValidator`, `DamageCalculator`, modelo mínimo de ataque/energía/tipos/debilidad/resistencia y eventos de ataque/daño.
- Dependencias: turnos.
- Riesgos: no interpretar efectos textuales; energías especiales solo cuentan si tienen `EnergyProfile` explícito.
- Criterio: ataque simple con energía suficiente, daño base, debilidad, resistencia, contadores y fin automático de turno testeado.

Decisiones fase 7:

- El ataque se declara desde `MAIN` y el engine transiciona internamente a `ATTACK` antes de finalizar turno.
- La energía no se consume al atacar.
- El validador cubre primero costes específicos y después `COLORLESS`.
- Debilidad se aplica antes que resistencia.
- Daño se acumula como contadores en `PokemonInPlay`; Fase 8 resuelve KO después del daño.

## Fase 8 - Knockout, premios y victoria

- Estado: implementada como motor puro Java, pendiente de ejecución local final de `mvn test` por restricción de entorno.
- Objetivo: resolver consecuencias básicas del combate y condiciones de finalización de partida.
- Entregables: `KnockoutResolver`, `PrizeResolver`, `PostAttackResolutionService`, `ActivePokemonReplacementResolver`, `VictoryConditionChecker`, eventos de KO/premios/victoria y resultado de partida.
- Dependencias: Fase 7 - ataques base.
- Riesgos: simultaneidades futuras, KO entre turnos por condiciones especiales, efectos que alteren premios y promoción obligatoria del Activo.
- Criterio: KO simple, descarte de evolución/attachments, premios normal/EX, último Premio, rival sin Pokémon, reemplazo de Activo, deck-out y simultaneidad representada cubiertos por tests.

Decisiones fase 8:

- KO se evalúa después de aplicar daño de ataque.
- Solo se resuelve KO del Activo defensor; daño a Banca queda fuera de alcance.
- El Pokémon noqueado, su pila de evolución y cartas unidas pasan al descarte del dueño.
- Pokémon normal otorga 1 Premio; Pokémon-EX otorga 2 mediante `CardSubtype.EX`.
- Los premios tomados se mueven desde `PrizeCards` a la mano del jugador que causó el KO.
- Si la partida continúa y el defensor tiene Banca, queda `PendingActiveReplacement` y no se finaliza turno hasta promover nuevo Activo.
- Deck-out deja de ser marcador provisional y produce victoria del oponente.
- Muerte Súbita queda representada como `GameFinishResult` de tipo `SUDDEN_DEATH_REQUIRED`; no se juega todavía el flujo completo.
- No se implementan condiciones especiales ni efectos complejos en esta fase.

## Fase 9 - Condiciones especiales y daño entre turnos

- Estado: implementada como motor puro Java, pendiente de ejecución local final de `mvn test` por restricción de entorno.
- Objetivo: soportar condiciones especiales oficiales e integrarlas con ataque, retiro, evolución, KO, premios y victoria.
- Entregables: `SpecialCondition`, `SpecialConditionSet`, `StatusEffectManager`, `BetweenTurnsService`, `CoinFlipProvider`, eventos de condiciones y tests unitarios/integración.
- Dependencias: Fase 6 turnos, Fase 7 ataques y Fase 8 KO/premios/victoria.
- Riesgos: timing entre turnos, KO por condición, Confusión auto-KO y no duplicar reglas de KO/victoria.
- Criterio: Dormido, Quemado, Confundido, Paralizado y Envenenado testeados; restricciones de ataque/retiro; limpieza por evolución/retiro; daño entre turnos integrado con KO/victoria.

Decisiones fase 9:

- Las condiciones especiales viven en `PokemonInPlay` mediante `SpecialConditionSet`.
- Dormido, Confundido y Paralizado son mutuamente excluyentes.
- Quemado y Envenenado coexisten con cualquier otra condición.
- Los chequeos de moneda usan `CoinFlipProvider` inyectable para tests deterministas.
- `TurnManager.endTurn` resuelve `BetweenTurnsService` antes de preparar el turno del oponente.
- El motor no interpreta texto libre de cartas XY1 ni habilidades en esta fase.

## Fase 10 - Motor de efectos de cartas

- Estado: implementada como arquitectura base de engine puro Java, pendiente de ejecución local final de `mvn test` por restricción de entorno.
- Objetivo: soportar efectos reales XY1 incrementalmente mediante arquitectura genérica auditable, incluyendo efectos simples de ataques y, más adelante, habilidades.
- Entregables: `EffectDefinition`, `EffectExecutionContext`, `EffectHandler`, `EffectRegistry`, `EffectExecutionService`, handlers genéricos iniciales y auditoría XY1 incremental.
- Dependencias: condiciones especiales, KO/premios/victoria y auditoría XY1.
- Riesgos: hardcode desordenado, interpretar texto natural como lógica ejecutable, declarar cobertura XY1 sin matriz ni tests.
- Criterio: cada efecto implementado está auditado y testeado; los efectos no mapeados permanecen explícitamente pendientes.

Decisiones fase 10:

- No se implementa parser de texto natural para cartas.
- La auditoría XY1 es la fuente de trazabilidad entre carta, efecto, handler, implementación y test.
- Se priorizan handlers genéricos reutilizables antes que custom por carta.
- La fase no equivale a cobertura completa del set XY1.
- Handlers iniciales: daño, curación, condición especial, robo, descarte de energía, moneda y composición.
- `AttackDefinition` acepta efectos estructurados sin romper el daño base existente.

## Fase 11 - Auditoría y mapeo progresivo XY1

- Estado: implementada como auditoría/mapping incremental de subset representativo, pendiente de ejecución local final de `mvn test` por restricción de entorno.
- Objetivo: auditar cartas reales `xy1`, clasificar efectos y mapear efectos soportados a `EffectDefinition` sin parser automático de texto natural.
- Entregables: matriz `docs/11-xy1-audit-matrix.md`, estados/categorías de auditoría, `Xy1EffectCatalog`, mappings por `cardId` + ataque, tests unitarios de mapping y test de ejecución representativa con `AttackService`.
- Dependencias: Fase 10 - motor de efectos.
- Riesgos: afirmar cobertura completa sin auditar 146 cartas, confundir texto de catálogo con lógica ejecutable, forzar Trainers/habilidades complejas en handlers genéricos incorrectos.
- Criterio: subset real mapeado/testeado, gaps documentados, auditoría explícitamente incompleta, sin WebSocket/frontend/persistencia/endpoints de juego.

Decisiones fase 11:

- `Xy1EffectCatalog` devuelve lista vacía para cartas/ataques sin mapping explícito; eso no significa que la carta no tenga efecto real.
- La auditoría completa de XY1 se declara `false` hasta revisar y testear el set completo.
- Se mapearon efectos representativos con handlers existentes: condición especial, curación, robo, moneda y descarte de energía.
- Trainers como `Professor's Letter` y `Shauna` quedan documentados como gaps porque requieren búsqueda/shuffle/manipulación de mano o mazo.
- Habilidades pasivas/continuas como `Spiky Shield` y `Sweet Veil` quedan como `REQUIRES_CUSTOM_HANDLER` / `NOT_IMPLEMENTED_YET`.

## Fase 12 - Persistencia de snapshots/logs

- Objetivo: guardar/reconstruir partida.
- Entregables: snapshots versionados, log inmutable.
- Dependencias: Game State estable y auditoría/mapping incremental para efectos ejecutables.
- Riesgos: pérdida de información oculta.
- Criterio: reconstrucción exacta en tests.

## Fase 13 - WebSockets

- Objetivo: sincronización realtime.
- Entregables: canales, eventos, reconexión, vistas seguras.
- Dependencias: persistencia y eventos.
- Riesgos: filtración o duplicados.
- Criterio: contrato WS testeado.

## Fase 14 - Tests fuertes

- Objetivo: cobertura y casos críticos.
- Entregables: unit/integration/WS/E2E mínimos.
- Dependencias: fases previas.
- Riesgos: tests frágiles.
- Criterio: JaCoCo >=80% y críticos >90%.

## Fase 15 - Preparación para frontend

- Objetivo: contratos estables para Angular.
- Entregables: OpenAPI, DTOs, eventos, vistas seguras.
- Dependencias: backend estable.
- Riesgos: cambiar contrato tarde.
- Criterio: frontend puede consumir comandos/vistas sin duplicar reglas.
