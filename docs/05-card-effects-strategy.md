# 05 - Card Effects Strategy

## Objetivo

Soportar efectos reales de cartas XY1 sin convertir el motor en una colección desordenada de `if cardId == ...`.

Estado Fase 11D: arquitectura base implementada con `EffectDefinition`, `EffectExecutionContext`, `EffectHandler`, `EffectRegistry`, `EffectExecutionService`, handlers genéricos iniciales e infraestructura de efectos continuos/modificadores. Además existe un primer catálogo explícito de mappings XY1 representativos (`Xy1EffectCatalog`) y una matriz de auditoría progresiva. No implica que todos los efectos XY1 estén mapeados, implementados o testeados.

## CardDefinition vs CardInstance

- `CardDefinition`: datos estáticos de pokemontcg.io/cache local. Incluye nombre, set, supertype, subtypes, ataques, habilidades, reglas, HP, debilidad, resistencia, coste de retirada y texto.
- `CardInstance`: copia concreta en una partida. Tiene ID único, zona, dueño, estado y vínculo con otros objetos de partida.

## EffectDefinition vs EffectExecution

- `EffectDefinition`: describe qué efecto tiene una carta o ataque de forma declarativa o semideclarativa.
- `EffectExecutionContext`: ejecución contextual del efecto sobre un `GameState`, con jugador, targets, fase, aleatoriedad controlada y eventos resultantes.

Regla obligatoria: `EffectDefinition` se carga desde una auditoría explícita o mapping mantenido por el equipo. No se genera automáticamente desde texto natural de cartas.

En Fase 11, los mappings reales auditados viven en `com.tpi.pokemon.game.engine.effect.mapping`. El catálogo inicial permite:

- buscar mappings por `cardId`;
- buscar efectos por `cardId` + `attackName` o `attackId`;
- devolver `List.of()` cuando una carta/ataque no tiene mapping explícito;
- declarar que la auditoría de XY1 no está completa.

## EffectRegistry

Registro interno que asocia `EffectType` con handlers.

Responsabilidades:

- Resolver handlers genéricos por categoría.
- Resolver handlers específicos cuando un efecto no entra en modelo genérico.
- Hacer explícito qué cartas XY1 están soportadas, testeadas o pendientes.

El registry puede resolver por categoría genérica y, solo cuando sea necesario, por handler custom específico. Un handler custom sin fila de auditoría y tests asociados no se considera terminado.

## EffectHandler / EffectExecutionService

Contrato conceptual:

```text
canHandle(effectDefinition, context)
validate(effectDefinition, context)
execute(effectDefinition, context) -> events + state changes
```

Los handlers no deben depender de Spring, JPA, WebSocket ni API externa.

`EffectExecutionService` resuelve cada `EffectDefinition` contra el registry y ejecuta listas de efectos en orden determinista. Los efectos de ataque se integran inicialmente después del daño base (`AFTER_DAMAGE` / `AFTER_ATTACK`).

## Efectos genéricos

- `DealDamageEffect`
- `ApplySpecialConditionEffect`
- `HealDamageEffect`
- `DrawCardsEffect`
- `DiscardAttachedEnergyEffect`
- `CoinFlipEffect`
- `CompositeEffect`
- `SearchDeckEffect`
- `ShuffleDeckEffect`
- `DiscardCardsEffect`
- `AttachEnergyEffect`
- `MoveEnergyEffect`
- `SwitchActiveEffect`
- `PlaceDamageCountersEffect`
- Infraestructura para `DamageModifierEffect`, `PreventDamageEffect`, `RetreatCostModifierEffect` y prevención de condiciones especiales mediante efectos continuos.

Pendientes o futuros:

- `NoOpEffect` para ataques cuyo único efecto actual sea daño base ya cubierto por `AttackService`.
- Mappings reales para `RetreatCostModifierEffect`, `DamageModifierEffect`, `PreventDamageEffect` y efectos preventivos.
- Persistencia temporal avanzada hasta fin de turno/próximo ataque.
- Habilidades activadas/pasivas/reactivas completas.
- Efectos continuos complejos de Tool/Stadium con mappings carta por carta.
- Contrato público de selección/reveal para frontend/API futura.

Prioridad recomendada para XY1:

1. Aplicar condiciones especiales desde ataques.
2. Robar cartas y descartar cartas simples.
3. Curación y contadores de daño directos.
4. Búsqueda en mazo, cambio de Activo y manipulación de Energía.
5. Efectos persistentes de Stadium/Tool/Habilidad.

## Fase 11C - Handlers directos

La Fase 11C agrega una primera tanda de handlers genéricos para desbloquear más categorías XY1, sin mapear todavía las 146 cartas completas:

- `SearchDeckEffectHandler`: mueve cartas seleccionadas del mazo a la mano, valida filtro simple y emite evidencia de búsqueda/reveal/shuffle requerido.
- `ShuffleDeckEffectHandler`: baraja mazo usando `DeckShuffler` inyectable.
- `DiscardCardsEffectHandler`: descarta cartas seleccionadas desde mano o mazo.
- `AttachEnergyEffectHandler`: adjunta Energía desde mano, descarte o mazo sin consumir la unión manual del turno.
- `MoveEnergyEffectHandler`: mueve Energía entre Pokémon propios seleccionados.
- `SwitchActiveEffectHandler`: cambia Activo por Banca cuando el target está resuelto; si falta selección, devuelve `PendingEffectSelection`.
- `PlaceDamageCountersEffectHandler`: coloca contadores directamente, separados del daño de ataque; integra KO/premios/victoria para Activo.

Limitaciones de Fase 11C:

- La selección pendiente queda modelada en `EffectResult`, no expuesta por API pública.
- No hay WebSocket, frontend, persistencia de partida ni endpoints REST de juego.
- No se implementan habilidades pasivas/reactivas/continuas ni modificadores globales de daño/retiro/prevent.
- `PlaceDamageCountersEffectHandler` resuelve KO del Activo; KOs complejos/distribuidos a Banca quedan para fases futuras.

## Fase 11D - Abilities, efectos continuos y modificadores

La Fase 11D agrega infraestructura genérica para representar fuentes de efectos persistentes sin hardcodear cartas por `cardId`:

- `CardEffectDefinition` en `CardDefinitionRef` para declarar efectos de carta separados de ataques.
- `EffectSourceCollector` para descubrir efectos desde Pokémon en juego, Tools adjuntas y Estadio activo.
- `ModifierResolver` para aplicar modificadores de daño, coste de retirada y prevención de condiciones especiales.
- Eventos de auditoría como `DamageModifiedEvent`, `DamagePreventedEvent`, `RetreatCostModifiedEvent` y `SpecialConditionPreventedEvent`.

Orden documentado para daño contextual:

1. daño base impreso;
2. modificadores `BEFORE_WEAKNESS_RESISTANCE`;
3. debilidad;
4. resistencia;
5. modificadores `AFTER_WEAKNESS_RESISTANCE`;
6. prevención;
7. clamp a `0` y múltiplos de 10.

Decisión importante: infraestructura disponible no equivale a soporte completo de una carta. `xy1-14 Chesnaught / Spiky Shield` y `xy1-95 Slurpuff / Sweet Veil` siguen requiriendo mapping explícito y tests antes de considerarse soportadas.

## Handlers custom

Usar solo cuando una carta XY1 no pueda modelarse razonablemente con efectos genéricos. Cada handler custom debe documentar:

- Carta/ataque/habilidad.
- Motivo por el que no alcanza un genérico.
- Casos de prueba obligatorios.
- Impacto en timing y privacidad.

## Categorías de efectos

| Categoría | Ejemplos |
|---|---|
| Daño | daño base, daño a banca, recoil, contadores directos |
| Condición | Dormido, Quemado, Confundido, Paralizado, Envenenado |
| Curación | remover contadores, curar por tipo/cantidad |
| Robo | robar N cartas, robar hasta condición |
| Búsqueda | buscar Pokémon/Energía/Trainer en mazo |
| Descarte | descartar mano, cartas del mazo, energías unidas |
| Energía | adjuntar, mover, contar o descartar energía |
| Cambio | cambiar activo propio/rival |
| Estadios | efectos persistentes globales |
| Herramientas | efectos persistentes unidos a Pokémon |
| Habilidades | activadas o pasivas |
| Persistentes | modificadores hasta fin de turno o mientras esté en juego |

## AS TÁCTICO / ACE SPEC

El set obligatorio `xy1` no contiene cartas AS TÁCTICO / ACE SPEC. Por lo tanto:

- No debe implementarse la validación "máximo 1 AS TÁCTICO / ACE SPEC por mazo" como parte del alcance base XY1.
- No debe tratarse ACE SPEC como categoría obligatoria en la auditoría XY1.
- Si el equipo incorpora opcionalmente otros sets que sí incluyan ACE SPEC, la regla debe agregarse como validación condicional por set/expansión y cubrirse con tests específicos.
- Al agregar un set opcional, también deben auditarse sus reglas particulares antes de habilitarlo para Deck Builder o Game Engine.

## Decisión explícita

No se intentará parsear automáticamente texto natural de cartas como primera estrategia. Primero se auditará XY1 y se mapearán efectos a handlers genéricos/custom. Parsear texto natural temprano es frágil, caro y poco verificable.

Esto también descarta documentar como implementado cualquier efecto inferido solo por leer `text`, `rules`, `attacks` o `abilities` del catálogo. Esos campos conservan fidelidad de datos, pero no son fuente ejecutable del motor.

## Criterio de cobertura

Una carta/efecto XY1 queda cubierto solo cuando cumple todo esto:

- Fila completa en `docs/11-xy1-audit-matrix.md`.
- Categoría y complejidad definidas.
- Handler genérico o custom identificado.
- Implementación existente en el engine.
- Tests unitarios o de integración que prueben timing, targets, eventos y mutación de estado.

Estados permitidos en la auditoría:

- `not_audited`: carta aún no clasificada.
- `audited`: efecto clasificado, sin implementación.
- `implemented`: handler disponible, falta o no consta test suficiente.
- `tested`: implementación cubierta por tests; carta considerada soportada.

## Matriz de auditoría XY1

La auditoría se mantiene en `docs/11-xy1-audit-matrix.md`. Debe indicar complejidad, categoría, handlers requeridos, estado de implementación y tests.

## Mappings XY1 iniciales

Fase 11 agrega mappings controlados para un subconjunto representativo, verificado contra datos oficiales del set `xy1`:

- `xy1-1 Venusaur-EX / Poison Powder`: aplica `POISONED` al Activo defensor después del daño.
- `xy1-1 Venusaur-EX / Jungle Hammer`: cura 30 al Activo atacante después del daño.
- `xy1-10 Pansage / Vine Whip`: daño base puro, sin `EffectDefinition` adicional.
- `xy1-10 Pansage / Leech Seed`: cura 10 al Activo atacante después del daño.
- `xy1-16 Spewpa / Stun Spore`: moneda; con cara aplica `PARALYZED` al Activo defensor.
- `xy1-68 Sableye / Filch`: roba 1 carta.
- `xy1-68 Sableye / Rip Claw`: moneda; con cara descarta 1 Energía del Activo defensor.

## Fase 11E.1 - Mapeo progresivo de Pokémon XY1

Fase 11E.1 agrega 17 mappings de ataques Pokémon verificados contra datos oficiales de `xy1`, usando solo handlers ya existentes:

- DAMAGE_ONLY: `Ledyba / Spinning Attack`, `Scatterbug / Bug Bite`, `Spewpa / Bug Bite`, `Skiddo / Tackle`, `Magcargo / Heat Blast`, `Pansear / Live Coal`, `Fennekin / Will-O-Wisp`, `Fletchinder / Fire Wing`, `Shellder / Rain Splash`.
- DAMAGE_PLUS_STATUS: `M Venusaur-EX / Crisis Vine`, `Beedrill / Poison Jab`, `Volbeat / Signal Beam`.
- DAMAGE_PLUS_HEAL: `Chesnaught / Touchdown`.
- DAMAGE_PLUS_COIN_FLIP: `Blastoise-EX / Splash Bomb`, `Cloyster / Clamp Crush`.
- DISCARD_ENERGY: `Slugma / Flamethrower`, `Pansear / Fireworks`, más descarte condicional dentro de `Cloyster / Clamp Crush`.

No se afirma cobertura completa de XY1: ataques como `Flash Needle`, `Lead`, `Magma Mantle`, `Rapid Spin`, `Spike Cannon`, `Spiky Shield` y `Sweet Veil` siguen marcados como pendientes cuando requieren selección, daño variable, prevención futura, habilidades reactivas o custom handlers.

Gaps documentados, no implementados como soporte completo:

- `xy1-123 Professor's Letter`: requiere búsqueda en mazo, reveal y shuffle.
- `xy1-127 Shauna`: requiere mezclar mano en mazo y robar 5; `DrawCardsEffectHandler` solo no alcanza.
- `xy1-14 Chesnaught / Spiky Shield`: habilidad pasiva/reactiva al recibir daño.
- `xy1-95 Slurpuff / Sweet Veil`: efecto continuo/preventivo condicionado por Energía Fairy.

## Cómo agregar un nuevo mapping

1. Verificar la carta contra catálogo local importado o fuente oficial de `xy1`.
2. Agregar/actualizar fila en `docs/11-xy1-audit-matrix.md` con categoría, complejidad, handlers y estado real.
3. Si entra en handlers genéricos, agregar un `AttackEffectMapping` en `Xy1EffectCatalog`.
4. Si no entra, marcar `REQUIRES_CUSTOM_HANDLER` / `NOT_IMPLEMENTED_YET`; no forzar el efecto en un handler incorrecto.
5. Agregar tests de lookup y, cuando corresponda, test de ejecución con `AttackService` o `EffectExecutionService`.
6. Recién marcar `FULLY_TESTED` cuando exista evidencia de test.

## Estrategia para completar XY1 al 100%

La subfase `feature/xy1-full-audit` agrega una herramienta interna de auditoría estática desde catálogo local:

- `Xy1AuditService` lee cartas `xy1` cacheadas desde `CardRepository`.
- `Xy1AuditReportGenerator` arma un reporte agregado.
- `Xy1CardClassifier` clasifica ataques, habilidades y reglas desde `attacks`, `abilities` y `rules` como JSON/texto de evidencia.
- La clasificación usa heurísticas revisables, pero **no ejecuta reglas ni genera mappings automáticamente**.

Orden recomendado para completar soporte:

1. Importar/cachear las 146 cartas con `POST /api/cards/import/xy1`.
2. Generar `Xy1AuditReport` desde `Xy1AuditService.generateReportFromLocalCache()`.
3. Resolver primero `DAMAGE_ONLY`, `DAMAGE_PLUS_STATUS`, `DAMAGE_PLUS_HEAL`, `DRAW_CARDS`, `DISCARD_ENERGY` porque ya tienen soporte genérico parcial o completo.
4. Implementar luego handlers reutilizables para `SEARCH_DECK`, `DISCARD_CARD`, `SWITCH_ACTIVE`, `ATTACH_ENERGY`, `MOVE_ENERGY` y `MODIFY_DAMAGE`.
5. Dejar para después efectos persistentes: `TOOL_EFFECT`, `STADIUM_EFFECT`, `ABILITY_PASSIVE`, `CONTINUOUS_EFFECT`, `PREVENT_DAMAGE`, `MODIFY_RETREAT_COST`.
6. Solo crear handlers custom cuando una carta real no encaje razonablemente en un genérico.

Handlers/infraestructura faltante detectada por categoría:

- Búsqueda/reveal/shuffle de mazo.
- Descarte de cartas de mano/mazo no energía.
- Adjuntar/mover energía por efecto.
- Cambio de Activo propio/rival.
- Colocar contadores de daño directamente.
- Modificar daño hecho/recibido.
- Prevenir daño/efectos.
- Modificar coste de retirada.
- Habilidades activadas, pasivas y reactivas.
- Efectos continuos de Tools y Stadiums.
- Selección de objetivo desde zonas ocultas con privacidad.

Riesgos:

- Confundir carta clasificada con carta implementada.
- Marcar `FULLY_TESTED` sin test real de mapping/ejecución.
- Clasificar mal texto ambiguo por heurística: toda fila conserva texto original y notas para revisión manual.
- Implementar búsqueda/reveal sin contrato de privacidad.
- Forzar Trainers/habilidades en handlers de ataque.
