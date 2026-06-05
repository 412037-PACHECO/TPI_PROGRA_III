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

- Estado: implementada como auditoría/mapping incremental y herramienta interna para generar auditoría completa desde cache local; pendiente de ejecución local final de `mvn test` por restricción de entorno.
- Objetivo: auditar cartas reales `xy1`, clasificar efectos y mapear efectos soportados a `EffectDefinition` sin parser automático de texto natural.
- Entregables: matriz `docs/11-xy1-audit-matrix.md`, estados/categorías de auditoría, `Xy1EffectCatalog`, mappings por `cardId` + ataque, `Xy1AuditService`, `Xy1AuditReportGenerator`, `Xy1CardClassifier`, modelos de reporte, tests unitarios de mapping/auditoría y test de ejecución representativa con `AttackService`.
- Dependencias: Fase 10 - motor de efectos.
- Riesgos: afirmar cobertura completa sin auditar 146 cartas, confundir texto de catálogo con lógica ejecutable, forzar Trainers/habilidades complejas en handlers genéricos incorrectos.
- Criterio: subset real mapeado/testeado, herramienta lista para auditar 146 cartas desde catálogo local, gaps documentados, auditoría de implementación explícitamente incompleta, sin WebSocket/frontend/persistencia/endpoints de juego.

Decisiones fase 11:

- `Xy1EffectCatalog` devuelve lista vacía para cartas/ataques sin mapping explícito; eso no significa que la carta no tenga efecto real.
- La auditoría completa de XY1 se declara `false` hasta revisar y testear el set completo.
- Se mapearon efectos representativos con handlers existentes: condición especial, curación, robo, moneda y descarte de energía.
- Trainers como `Professor's Letter` y `Shauna` fueron gaps históricos por búsqueda/shuffle/manipulación de mano o mazo; 11G.3 cierra los que podían resolverse internamente sin UI/API pública. 11G.3B agrega handlers internos para `Cassius`, `Evosoda`, `Great Ball` y `Super Potion`, manteniendo pendiente la exposición pública segura.
- Habilidades pasivas/continuas/reactivas se cierran de forma incremental: `Sweet Veil` queda completo en 11G.1 y `Spiky Shield` queda completo para su trigger acotado en 11G.2; eso no implica cobertura total de todas las habilidades XY1.
- La subfase `feature/xy1-full-audit` no implementa handlers faltantes; clasifica efectos desde cache local y genera reporte con `importedCardCount`, conteos por complejidad y gaps.
- Si no hay `backend/data`/cache importado, no se puede afirmar auditoría factual de las 146 cartas en ese entorno; debe importarse `xy1` antes de ejecutar el reporte completo.

### Fase 11C - Primera tanda de handlers faltantes

- Estado: implementada como handlers genéricos puros Java, pendiente de ejecución local final de Maven por restricción de entorno.
- Objetivo: desbloquear categorías directas detectadas en auditoría XY1 sin mapear todavía las 146 cartas completas.
- Entregables: `SearchDeckEffectHandler`, `ShuffleDeckEffectHandler`, `DiscardCardsEffectHandler`, `AttachEnergyEffectHandler`, `MoveEnergyEffectHandler`, `SwitchActiveEffectHandler`, `PlaceDamageCountersEffectHandler`, eventos mínimos y tests unitarios.
- Fuera de alcance: habilidades pasivas/reactivas/continuas, efectos persistentes de Tool/Stadium, prevent/damage/retreat modifiers globales, WebSocket, frontend, persistencia, endpoints de juego y parser automático de texto.
- Riesgos: confundir handler disponible con carta soportada, filtrar información de zonas ocultas, resolver selección futura sin contrato público y duplicar lógica de KO.
- Criterio: handlers testeados aisladamente, selección pendiente modelada, categorías documentadas y matriz sin afirmar cobertura completa.

### Fase 11D - Abilities, continuous effects y modificadores

- Estado: infraestructura mínima implementada como engine puro Java, pendiente de ejecución local final de Maven por restricción de entorno.
- Objetivo: habilitar efectos continuos y modificadores reutilizables sin mapear todavía las 146 cartas XY1 ni interpretar texto natural.
- Entregables: `CardEffectDefinition`, modelos de ability/source/scope/condition, `EffectSourceCollector`, `ModifierResolver`, modificadores de daño/retiro/condiciones especiales, eventos de modificación/prevención y tests unitarios mínimos.
- Fuera de alcance: mappings completos de XY1, abilities activadas con uso una vez por turno, reactive effects completos como `Spiky Shield`, cleanup continuo completo como `Sweet Veil`, WebSocket, frontend, persistencia, endpoints de juego y parser automático.
- Riesgos: confundir infraestructura con carta soportada, orden incorrecto de modificadores, stacking simultáneo de múltiples fuentes y semántica inicial simple de scopes/conditions.
- Criterio: comportamiento no-op compatible con fases previas, daño/retiro/prevención testeados, sin dependencias Spring/JPA/API dentro del engine.

### Fase 11E.5 - Casos complejos/custom restantes XY1

- Estado: implementada parcialmente como cierre incremental honesto, pendiente de ejecución local final de Maven por restricción de entorno.
- Objetivo: cerrar gaps puntuales que podían resolverse con infraestructura mínima y documentar el backlog complejo restante sin afirmar cobertura total de XY1.
- Entregables: `EnergyProfile.rainbow()`, pago de un único símbolo flexible para Rainbow Energy, trigger básico de contador al adjuntar desde mano, mapping/test de `Fairy Garden`, metadata ampliada en `PendingEffectSelection`, matriz/docs actualizadas.
- Fuera de alcance: frontend, WebSocket, persistencia, endpoints REST de juego, parser automático, contratos públicos de selección/privacidad y cierre total de todas las cartas XY1.
- Gaps derivados inicialmente a 11F: `Shadow Circle`, `Spiky Shield`, cleanup completo de `Sweet Veil`, Trainers con zonas ocultas/top-N/mano completa y handlers custom de carta completa. Los tres primeros se cierran luego en 11G.1/11G.2.
- Criterio: gaps cerrados tienen mapping y tests; gaps restantes tienen motivo técnico explícito y no se marcan `FULLY_TESTED`.

### Fase 11G.1 - Cierre de gaps críticos XY1

- Estado: implementada en engine puro Java; pendiente de ejecución local final de Maven por restricción de entorno.
- Objetivo: cerrar tres gaps críticos sin tocar frontend, WebSocket, persistencia, endpoints REST de juego ni parser automático: `Rainbow Energy` KO/premios/victoria, cleanup completo de `Sweet Veil` y `Shadow Circle` como prevención de Weakness.
- Entregables: `PREVENT_WEAKNESS` en `ModifierResolver`, integración de KO de Banca en `PostAttackResolutionService`, reconciliación de condiciones prevenidas en `StatusEffectManager`, mapping de `xy1-126 Shadow Circle`, actualización de estados para `xy1-95` y `xy1-131`, tests de acción/daño/catálogo.
- Fuera de alcance: `Spiky Shield`, Trainers complejos con zonas ocultas/top-N/mano completa, UI/API pública de selección/reveal, auditoría real completa de 146 cartas sin cache local.
- Criterio: Rainbow desde mano puede noquear Pokémon propio Activo o de Banca usando servicios existentes de KO/premios/victoria; Sweet Veil previene y remueve condiciones para Pokémon propios con Energía Fairy-providing; Shadow Circle suprime Weakness solo para Pokémon con Energía Darkness-providing y mantiene Resistance.

### Fase 11G.2 - Spiky Shield e infraestructura reactiva mínima

- Estado: implementada en engine puro Java; pendiente de ejecución local final de Maven por restricción de entorno.
- Objetivo: cerrar `xy1-14 Chesnaught / Spiky Shield` mediante resolución reactiva acotada para habilidades que responden a daño recibido por ataque rival.
- Entregables: mapping completo de `Spiky Shield`, infraestructura/contexto/resolver reactivo para `ON_DAMAGE_RECEIVED`, colocación de 3 contadores sobre el atacante original, integración con KO/premios/victoria y tests del timing.
- Fuera de alcance: frontend, WebSocket, persistencia, endpoints REST de juego, parser automático, Trainers complejos, selección/reveal/privacidad y sistema universal de todos los triggers.
- Criterio: Spiky Shield se ejecuta cuando Chesnaught Activo recibe daño positivo de ataque rival, aplica contadores al atacante incluso si Chesnaught queda KO, no dispara ante fuentes incorrectas y cualquier KO derivado usa los servicios existentes.

### Fase 11G.3 - Trainers complejos internos

- Estado: implementada parcialmente en engine puro Java; pendiente de ejecución local final de Maven por restricción de entorno.
- Objetivo: cerrar Trainers XY1 complejos que pueden resolverse de forma segura sin frontend, WebSocket, persistencia, endpoints REST ni parser automático.
- Entregables: `DiscardHandDrawEffectHandler`, `ShuffleHandIntoDeckDrawEffectHandler`, `PutDiscardPokemonOnTopDeckEffectHandler`, eventos de shuffle/move top-deck, mappings para `Professor Sycamore`, `Shauna`, `Red Card`, `Max Revive` y cierre interno de `Professor's Letter`.
- Fuera de alcance: contratos públicos de selección/reveal/privacidad y vistas seguras por jugador.

### Fase 11G.3B - Trainers restantes con selección compleja

- `Cassius`: handler interno para devolver Pokémon propio + cartas unidas al mazo y generar reemplazo activo pendiente si corresponde.
- `Evosoda`: handler interno para evolución directa desde mazo con validación `evolvesFrom` y shuffle.
- `Great Ball`: handler interno para inspección top-7, elección opcional de Pokémon, reveal y shuffle.
- `Super Potion`: handler interno para curar hasta 60 y descartar una Energía unida.
- Pendiente: ejecutar suite local y diseñar contrato UI/API/WebSocket de selección segura antes de considerar cobertura pública completa.

### Fase 11G.4 - Verificación carta por carta XY1

- Verifica los 146 IDs oficiales del set `xy1` contra fuente oficial descargada de `pokemontcg.io` v2.
- Agrega matriz de trazabilidad completa en `docs/12-xy1-card-by-card-verification.md`.
- Resultado actual: 47 cartas tienen fila detallada previa y 99 quedan con `PENDING_ROW_CREATION` para 11G.5.
- Corrige criterios de completitud: no marcar `FULLY_TESTED` si la carta completa depende de UI/API futura, selección pública segura o tiene ataques/abilities pendientes.
- 11F queda bloqueada por 11G.5 si el equipo necesita reporte final real de cumplimiento XY1 completo.

### Fase 11G.5 - Clasificación completa de cartas pendientes

- Completa la clasificación documental de las 99 cartas que en 11G.4 estaban como `PENDING_ROW_CREATION`.
- La matriz `docs/12-xy1-card-by-card-verification.md` ahora enumera y clasifica las 146 cartas oficiales.
- Resultado de las 99 nuevas: 6 `DAMAGE_ONLY_SUPPORTED`, 14 `PARTIAL_SUPPORT`, 28 `REQUIRES_UI_SELECTION`, 34 `REQUIRES_CUSTOM_HANDLER`, 17 `NOT_IMPLEMENTED_YET`.
- No agrega handlers ni aumenta `FULLY_TESTED`; solo identifica gaps reales.
- En ese momento 11G.6 quedaba recomendada antes de 11F; el follow-up implementado cerró solo damage-only de bajo riesgo, por lo que soporte jugable completo aún requiere un pase posterior de Game Engine y selección pública.
- Criterio: no se elige automáticamente cuando hay elección del jugador; pending selections conservan metadata; hand-to-deck shuffle no revela manos; mappings cerrados tienen tests y gaps restantes quedan explícitos.

### Fase 11G.6 - Follow-up damage-only de bajo riesgo

- Estado: implementada como cierre acotado de Game Engine puro; pendiente de ejecución local final de Maven por restricción de entorno.
- Objetivo: cerrar únicamente los 6 casos `DAMAGE_ONLY_SUPPORTED` detectados en 11G.5 mediante mappings vacíos explícitos, sin handlers nuevos ni contrato público.
- Entregables: mappings/audit entries para `xy1-47 Ekans`, `xy1-49 Spoink`, `xy1-69 Sandile`, `xy1-83 Honedge`, `xy1-94 Swirlix` y `xy1-108 Lillipup`; tests de catálogo que verifican categoría `DAMAGE_ONLY`, `EFFECT_MAPPED` y `FULLY_TESTED` para ese alcance.
- Resultado honesto: `FULLY_TESTED` sube de 33 a 39; `DAMAGE_ONLY_SUPPORTED` pendiente baja de 6 a 0; quedan 107 cartas con gap real o soporte parcial.
- Fuera de alcance: daño variable, efectos `next turn`, búsqueda+attach desde mazo, switch coordinado, habilidades activadas, selección pública segura, frontend, WebSocket, persistencia y endpoints REST de juego.

### Fase 11F - Reporte final de cumplimiento XY1

- Objetivo: consolidar reporte final de cumplimiento/gaps del set XY1 y priorizar custom handlers o contratos públicos faltantes.
- Entregables recomendados: reporte por carta/efecto, lista de gaps por dependencia técnica, criterios de demo académica, y plan de cierre para selección/reveal/shuffle, triggers reactivos, Weakness/retreat/stadium avanzados y efectos de Trainers complejos.
- Dependencias: Fases 11E.1 a 11E.5 y cache local XY1 importado.
- Criterio: no declarar 100% XY1 salvo que las 146 cartas estén auditadas, mapeadas cuando corresponda y testeadas con evidencia.

## Fase 12 - Persistencia de snapshots/logs

- Objetivo: guardar y reconstruir una partida completa para cubrir RF-03/RF-05 sin acoplar el engine a JPA.
- Entregables: metadata consultable de partida (`GameSessionEntity`), snapshots JSON completos de `GameState` (`GameSnapshotEntity`), log inmutable append-only con `sequence` (`GameActionLogEntity`), repositorios/servicio de aplicación para persistir después de acciones relevantes y reconstrucción desde último snapshot.
- Dependencias: Game State estable y auditoría/mapping incremental para efectos ejecutables.
- Riesgos: pérdida de información oculta, filtración de zonas privadas si se expone el snapshot crudo, incompatibilidad al cambiar el formato de `GameState`, secuencias duplicadas o gaps por concurrencia.
- Criterio: reconstrucción exacta en tests desde último snapshot + logs posteriores; metadata coincide con el estado reconstruido; logs son append-only; el engine sigue sin depender de entidades JPA/Spring/API.

Decisiones fase 12:

- No se normaliza todo el estado interno en tablas relacionales; el `GameState` completo se guarda como JSON versionado para evitar duplicar el modelo mutable del engine.
- Se normaliza solo lo necesario para consulta operativa: `gameId`, jugadores, estado, timestamps, ganador/resultado, turno/fase y `sequence`.
- Las entidades de persistencia (`GameSessionEntity`, `GameSnapshotEntity`, `GameActionLogEntity`) quedan separadas de los modelos del engine.
- El log no reemplaza al snapshot: explica y audita la transición, mientras el snapshot acelera recuperación/reconexión.
- No se agregan todavía endpoints REST de juego, WebSocket, frontend ni vistas seguras por jugador; quedan como próximo paso de API de partidas.

## Fase 13 - API REST básica de partidas

- Estado: implementada como contrato mínimo de sesión/auditoría; pendiente de ejecución local final de Maven por restricción de entorno.
- Objetivo: exponer una capa REST de aplicación sobre la persistencia sin meter reglas en controllers ni publicar vistas inseguras.
- Entregables: `GameApplicationService`, `GameQueryService`, `GameController`, DTOs REST, excepciones application y mapeo de errores en `GlobalExceptionHandler`.
- Endpoints implementados: `POST /api/games`, `POST /api/games/{gameId}/join`, `GET /api/games/{gameId}`, `GET /api/games/waiting`, `GET /api/games/{gameId}/log`.
- Decisión: crear sala `WAITING` persiste solo metadata; al unirse segundo jugador se toma lock pesimista sobre la sesión, se crea `GameState.CREATED`, snapshot inicial y log `GAME_JOINED`.
- Fuera de alcance: setup desde mazos, acciones de turno, ataque, resolución de selecciones pendientes, WebSocket, frontend y vistas seguras finales por jugador.
- Riesgos: el log crudo puede contener información privada; no debe usarse como contrato final de frontend.
- Criterio: endpoints delegan a application services, validan entrada básica, persisten snapshot/log al crear GameState inicial y tienen tests de application/API.

## Fase 14 - Vistas seguras por jugador

- Estado: implementada como capa de proyección application, pendiente de ejecución local final de Maven por restricción de entorno.
- Objetivo: devolver estado de partida desde la perspectiva de cada jugador sin exponer `GameState` crudo ni datos ocultos del rival.
- Entregables: DTOs en `game/application/view`, `GameViewProjectionService`, `GameLogProjectionService`, endpoints `GET /api/games/{gameId}/view?viewerPlayerId=...` y `GET /api/games/{gameId}/log?viewerPlayerId=...`.
- Reglas de visibilidad: mano propia visible, mano rival solo conteo; mazos sin orden; premios boca abajo solo conteo; descarte público; campo público de ambos jugadores visible; selecciones pendientes privadas solo para el jugador autorizado.
- Decisión: `GET /api/games/{gameId}` sigue devolviendo metadata para compatibilidad; `/view` es el contrato recomendado para frontend/reconexión.
- Decisión: `GET /api/games/{gameId}/log` devuelve solo log público/sanitizado; el log crudo queda como servicio interno de auditoría/debug, no endpoint público.
- Decisión: `viewerPlayerId` es selector de perspectiva en esta fase sin autenticación completa; cuando exista seguridad debe derivarse de sesión/token.
- Fuera de alcance: WebSocket, frontend, autenticación completa, endpoints completos de gameplay y nuevas reglas de cartas.
- Riesgos: cualquier DTO nuevo de partida debe pasar por proyección segura; nunca devolver snapshots ni logs crudos como contrato frontend.
- Criterio: tests prueban que mano rival, orden de mazo, premios ocultos y candidatos privados de pending selection no se filtran.

## Fase 15 - WebSockets

- Objetivo: sincronización realtime.
- Entregables: canales, eventos, reconexión y emisión de las vistas seguras por jugador de Fase 14.
- Dependencias: persistencia, eventos y vistas seguras.
- Riesgos: filtración o duplicados.
- Criterio: contrato WS testeado.

## Fase 16 - Tests fuertes

- Objetivo: cobertura y casos críticos.
- Entregables: unit/integration/WS/E2E mínimos.
- Dependencias: fases previas.
- Riesgos: tests frágiles.
- Criterio: JaCoCo >=80% y críticos >90%.

## Fase 17 - Preparación para frontend

- Objetivo: contratos estables para Angular.
- Entregables: OpenAPI, DTOs, eventos, vistas seguras.
- Dependencias: backend estable.
- Riesgos: cambiar contrato tarde.
- Criterio: frontend puede consumir comandos/vistas sin duplicar reglas.
