# 11 - XY1 Audit Matrix

## Objetivo

Auditar progresivamente las cartas del set obligatorio `xy1`, clasificar sus efectos reales y mapear un subconjunto representativo a `EffectDefinition` sin parser automático de texto natural.

Esta matriz **no representa todavía cobertura completa implementada de las 146 cartas XY1**. La subfase `feature/xy1-full-audit` agrega una herramienta interna para auditar las 146 cartas desde el catálogo local cacheado y clasificar efectos sin ejecutarlos.

Fuente esperada para auditoría completa: campos oficiales `attacks`, `abilities`, `rules`, `subtypes`, `supertype` y `rawJson` cacheados en `CardEntity` para `set.id=xy1`. En este entorno no hay `backend/data` local disponible, por lo tanto no se puede afirmar que las 146 cartas hayan sido auditadas desde DB local. La herramienta queda lista para generar el reporte completo después de importar `xy1`.

## Subfase: herramienta de auditoría completa XY1

Estado real actual:

- Set objetivo: `xy1`.
- Cantidad esperada: 146 cartas.
- Cache local disponible en este entorno: no (`backend/data` ausente durante la implementación).
- Auditoría ejecutable completa desde cache local: pendiente de importar datos.
- Fuente de ejecución de efectos: mappings explícitos en `Xy1EffectCatalog`, no texto natural.

Herramienta interna agregada:

- `Xy1AuditService`: servicio interno Spring, sin endpoint público, que lee cartas `xy1` desde `CardRepository`.
- `Xy1AuditReportGenerator`: genera reporte desde una lista de `CardEntity`.
- `Xy1CardClassifier`: clasifica ataques, habilidades y reglas desde JSON/texto de catálogo.
- Modelos de reporte: `Xy1AuditReport`, `Xy1CardAuditEntry`, `Xy1AttackAuditEntry`, `Xy1AbilityAuditEntry`, `Xy1RuleAuditEntry`, `Xy1UnsupportedEffectReport`.

Proceso para auditar las 146 cartas:

1. Levantar backend con perfil local.
2. Importar catálogo XY1:

   ```http
   POST /api/cards/import/xy1
   ```

3. Verificar que el resumen indique 146 cartas recibidas/cacheadas para `setId=xy1`.
4. Ejecutar `Xy1AuditService.generateReportFromLocalCache()` desde test, runner interno o futura tarea administrativa.
5. Exportar el `Xy1AuditReport` a matriz Markdown/CSV si el equipo decide automatizar la documentación.
6. No marcar cierre completo hasta que el reporte tenga `importedCardCount == 146` y todos los gaps estén clasificados.

## Estados de implementación

- `DATA_IMPORTED`: la carta existe en catálogo/cache local o fue verificada contra fuente oficial.
- `EFFECT_CLASSIFIED`: el efecto fue leído y categorizado.
- `EFFECT_SUPPORTED_BY_GENERIC_HANDLER`: el efecto entra en handlers genéricos actuales.
- `EFFECT_MAPPED`: existe mapping estructurado hacia `EffectDefinition`.
- `FULLY_TESTED`: existen tests del mapping/ejecución para el alcance declarado.
- `REQUIRES_CUSTOM_HANDLER`: el efecto no entra razonablemente en genéricos actuales.
- `NOT_IMPLEMENTED_YET`: efecto válido de XY1, pendiente de soporte/mapping/test.
- `OUT_OF_SCOPE_FOR_XY1`: regla/efecto no aplicable al set obligatorio XY1.

Una carta no debe marcarse `FULLY_TESTED` solo porque exista un handler genérico. Debe existir mapping explícito y test del comportamiento declarado.

## Categorías de efectos

- `DAMAGE_ONLY`
- `DAMAGE_PLUS_STATUS`
- `DAMAGE_PLUS_HEAL`
- `DAMAGE_PLUS_COIN_FLIP`
- `APPLY_STATUS`
- `HEAL_DAMAGE`
- `DRAW_CARDS`
- `DISCARD_ENERGY`
- `DISCARD_CARD`
- `SEARCH_DECK`
- `SWITCH_ACTIVE`
- `ATTACH_ENERGY`
- `MOVE_ENERGY`
- `TOOL_EFFECT`
- `STADIUM_EFFECT`
- `ABILITY_ACTIVATED`
- `ABILITY_PASSIVE`
- `CONTINUOUS_EFFECT`
- `PREVENT_DAMAGE`
- `MODIFY_DAMAGE`
- `MODIFY_RETREAT_COST`
- `CUSTOM_REQUIRED`
- `UNSUPPORTED_YET`

## Handlers genéricos disponibles

- `DealDamageEffectHandler`
- `HealDamageEffectHandler`
- `ApplySpecialConditionEffectHandler`
- `DrawCardsEffectHandler`
- `DiscardAttachedEnergyEffectHandler`
- `CoinFlipEffectHandler`
- `CompositeEffectHandler`

La existencia de un handler no implica cobertura completa de carta. La lógica base de daño sigue en `AttackService`; los mappings solo agregan efectos secundarios estructurados.

## Matriz inicial Fase 11

La tabla siguiente sigue siendo el subset explícitamente mapeado/documentado. Para convertirla en matriz completa de 146 filas debe generarse el reporte desde cache local importado y volcar sus resultados.

| cardId | name | supertype | subtypes | attacks | abilities | rules | effectText | effectCategory | complexity | supportedByCurrentEngine | genericHandlers | customHandlerRequired | implementationStatus | tested | notes |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| xy1-1 | Venusaur-EX | Pokémon | Basic, EX | Poison Powder; Jungle Hammer | none | Pokémon-EX rule | Poison Powder: `Your opponent's Active Pokémon is now Poisoned.` Jungle Hammer: `Heal 30 damage from this Pokémon.` | DAMAGE_PLUS_STATUS; DAMAGE_PLUS_HEAL | MEDIUM | yes | ApplySpecialConditionEffectHandler; HealDamageEffectHandler | no | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_SUPPORTED_BY_GENERIC_HANDLER; EFFECT_MAPPED; FULLY_TESTED | yes | Mapping creado para ambos ataques. El daño base queda en `AttackDefinition`; la regla EX ya se soporta por subtype EX en premios. |
| xy1-10 | Pansage | Pokémon | Basic | Vine Whip; Leech Seed | none | none | Vine Whip: daño base sin texto. Leech Seed: `Heal 10 damage from this Pokémon.` | DAMAGE_ONLY; DAMAGE_PLUS_HEAL | LOW | yes | AttackService; HealDamageEffectHandler | no | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_SUPPORTED_BY_GENERIC_HANDLER; EFFECT_MAPPED; FULLY_TESTED | yes | `Vine Whip` devuelve lista vacía de efectos por ser daño puro; `Leech Seed` mapea curación. |
| xy1-16 | Spewpa | Pokémon | Stage 1 | Bug Bite; Stun Spore | none | none | Stun Spore: `Flip a coin. If heads, your opponent's Active Pokémon is now Paralyzed.` | DAMAGE_ONLY; DAMAGE_PLUS_COIN_FLIP; DAMAGE_PLUS_STATUS | MEDIUM | yes | CoinFlipEffectHandler; ApplySpecialConditionEffectHandler | no | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_SUPPORTED_BY_GENERIC_HANDLER; EFFECT_MAPPED; FULLY_TESTED | yes | Mapping creado con rama heads Paralyzed y tails sin efecto secundario. |
| xy1-68 | Sableye | Pokémon | Basic | Filch; Rip Claw | none | none | Filch: `Draw a card.` Rip Claw: `Flip a coin. If heads, discard an Energy attached to your opponent's Active Pokémon.` | DRAW_CARDS; DAMAGE_PLUS_COIN_FLIP; DISCARD_ENERGY | MEDIUM | yes | DrawCardsEffectHandler; CoinFlipEffectHandler; DiscardAttachedEnergyEffectHandler | no | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_SUPPORTED_BY_GENERIC_HANDLER; EFFECT_MAPPED; FULLY_TESTED | yes | Mapping creado para robo y descarte condicional de energía. |
| xy1-123 | Professor's Letter | Trainer | Item | none | none | Item | `Search your deck for up to 2 basic Energy cards, reveal them, and put them into your hand. Shuffle your deck afterward.` | SEARCH_DECK | MEDIUM | no | none | no/tbd | DATA_IMPORTED; EFFECT_CLASSIFIED; NOT_IMPLEMENTED_YET | no | Requiere `SearchDeckEffect`, reveal, shuffle y tratamiento de zona oculta. |
| xy1-127 | Shauna | Trainer | Supporter | none | none | Supporter rule | `Shuffle your hand into your deck. Then, draw 5 cards.` | DRAW_CARDS; DISCARD_CARD; CUSTOM_REQUIRED | MEDIUM | no | DrawCardsEffectHandler partial only | yes/tbd | DATA_IMPORTED; EFFECT_CLASSIFIED; REQUIRES_CUSTOM_HANDLER; NOT_IMPLEMENTED_YET | no | No alcanza con robar 5: primero debe mezclar mano en mazo y barajar. |
| xy1-14 | Chesnaught | Pokémon | Stage 2 | Touchdown | Spiky Shield | none | Ability: al recibir daño de ataque, pone 3 contadores en el atacante. Touchdown cura 20. | ABILITY_PASSIVE; CONTINUOUS_EFFECT; DAMAGE_PLUS_HEAL | HIGH | partial | HealDamageEffectHandler partial only | yes | DATA_IMPORTED; EFFECT_CLASSIFIED; REQUIRES_CUSTOM_HANDLER; NOT_IMPLEMENTED_YET | no | Gap representativo: habilidad reactiva `on damaged by attack` no soportada por timing actual. |
| xy1-95 | Slurpuff | Pokémon | Stage 1 | Draining Kiss | Sweet Veil | none | Ability: Pokémon propios con Energía Fairy no pueden ser afectados por condiciones especiales; remueve condiciones. Draining Kiss cura 30. | ABILITY_PASSIVE; CONTINUOUS_EFFECT; DAMAGE_PLUS_HEAL | HIGH | partial | HealDamageEffectHandler partial only | yes | DATA_IMPORTED; EFFECT_CLASSIFIED; REQUIRES_CUSTOM_HANDLER; NOT_IMPLEMENTED_YET | no | Gap representativo de efecto continuo/preventivo condicionado por energía unida. |

## Reglas de auditoría

1. No interpretar `effectText`, `rules`, `attacks` ni `abilities` como lógica ejecutable.
2. No marcar cobertura completa de XY1 hasta auditar las 146 cartas y testear efectos implementados.
3. Si un ataque solo hace daño base, puede quedar auditado con mapping vacío y nota `DAMAGE_ONLY`.
4. Si un efecto requiere buscar/revelar desde mazo, documentar privacidad y selección.
5. Si un efecto requiere persistencia temporal, habilidad pasiva, reemplazo forzado, búsqueda, descarte de mano/mazo o modificación de reglas, marcar gap antes de implementar.

## Gaps detectables por la herramienta

Handlers/infraestructura faltante para completar XY1:

- `SearchDeckEffectHandler` y reveal de cartas buscadas.
- `ShuffleDeckEffectHandler`.
- `DiscardCardsEffectHandler` para mano/mazo no energía.
- `AttachEnergyEffectHandler`.
- `MoveEnergyEffectHandler`.
- `SwitchActiveEffectHandler`.
- `PlaceDamageCountersEffectHandler`.
- `DamageModifierEffectHandler`.
- `PreventDamageEffectHandler`.
- `RetreatCostModifierEffectHandler`.
- Infraestructura de habilidades activadas.
- Infraestructura de habilidades pasivas/reactivas.
- Efectos continuos de Tool/Stadium.
- Selección desde zonas ocultas, reveal y privacidad de mano/mazo/premios.
- Timing avanzado: before damage, after damage, between turns, on damaged, while in play, next turn.

## Aclaración ACE SPEC

El set obligatorio `xy1` no contiene cartas AS TÁCTICO / ACE SPEC. La validación “máximo 1 ACE SPEC por mazo” queda fuera del alcance obligatorio XY1 y solo debería agregarse si se habilitan sets futuros que sí incluyan esa mecánica.
