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
- `SearchDeckEffectHandler`
- `ShuffleDeckEffectHandler`
- `DiscardCardsEffectHandler`
- `AttachEnergyEffectHandler`
- `MoveEnergyEffectHandler`
- `SwitchActiveEffectHandler`
- `PlaceDamageCountersEffectHandler`
- `CoinFlipEffectHandler`
- `CompositeEffectHandler`

Infraestructura genérica disponible desde Fase 11D, todavía no equivalente a mapping completo de cartas:

- `CardEffectDefinition` para abilities/effects declarados por carta.
- `EffectSourceCollector` para Pokémon en juego, Tools y Stadium.
- `ModifierResolver` para `MODIFY_DAMAGE`, `PREVENT_DAMAGE`, `MODIFY_RETREAT_COST` y `PREVENT_SPECIAL_CONDITION`.

La existencia de un handler no implica cobertura completa de carta. La lógica base de daño sigue en `AttackService`; los mappings solo agregan efectos secundarios estructurados.

## Matriz inicial Fase 11

La tabla siguiente sigue siendo el subset explícitamente mapeado/documentado. Para convertirla en matriz completa de 146 filas debe generarse el reporte desde cache local importado y volcar sus resultados.

Fase 11E.1 agrega 17 mappings de ataques Pokémon XY1 verificados contra datos oficiales de `pokemontcg.io` v2, priorizando daño puro, condiciones, curación, moneda, descarte de Energía y composiciones ya soportadas.

| cardId | name | supertype | subtypes | attacks | abilities | rules | effectText | effectCategory | complexity | supportedByCurrentEngine | genericHandlers | customHandlerRequired | implementationStatus | tested | notes |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| xy1-1 | Venusaur-EX | Pokémon | Basic, EX | Poison Powder; Jungle Hammer | none | Pokémon-EX rule | Poison Powder: `Your opponent's Active Pokémon is now Poisoned.` Jungle Hammer: `Heal 30 damage from this Pokémon.` | DAMAGE_PLUS_STATUS; DAMAGE_PLUS_HEAL | MEDIUM | yes | ApplySpecialConditionEffectHandler; HealDamageEffectHandler | no | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_SUPPORTED_BY_GENERIC_HANDLER; EFFECT_MAPPED; FULLY_TESTED | yes | Mapping creado para ambos ataques. El daño base queda en `AttackDefinition`; la regla EX ya se soporta por subtype EX en premios. |
| xy1-10 | Pansage | Pokémon | Basic | Vine Whip; Leech Seed | none | none | Vine Whip: daño base sin texto. Leech Seed: `Heal 10 damage from this Pokémon.` | DAMAGE_ONLY; DAMAGE_PLUS_HEAL | LOW | yes | AttackService; HealDamageEffectHandler | no | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_SUPPORTED_BY_GENERIC_HANDLER; EFFECT_MAPPED; FULLY_TESTED | yes | `Vine Whip` devuelve lista vacía de efectos por ser daño puro; `Leech Seed` mapea curación. |
| xy1-16 | Spewpa | Pokémon | Stage 1 | Bug Bite; Stun Spore | none | none | Stun Spore: `Flip a coin. If heads, your opponent's Active Pokémon is now Paralyzed.` | DAMAGE_ONLY; DAMAGE_PLUS_COIN_FLIP; DAMAGE_PLUS_STATUS | MEDIUM | yes | CoinFlipEffectHandler; ApplySpecialConditionEffectHandler | no | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_SUPPORTED_BY_GENERIC_HANDLER; EFFECT_MAPPED; FULLY_TESTED | yes | Mapping creado con rama heads Paralyzed y tails sin efecto secundario. |
| xy1-68 | Sableye | Pokémon | Basic | Filch; Rip Claw | none | none | Filch: `Draw a card.` Rip Claw: `Flip a coin. If heads, discard an Energy attached to your opponent's Active Pokémon.` | DRAW_CARDS; DAMAGE_PLUS_COIN_FLIP; DISCARD_ENERGY | MEDIUM | yes | DrawCardsEffectHandler; CoinFlipEffectHandler; DiscardAttachedEnergyEffectHandler | no | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_SUPPORTED_BY_GENERIC_HANDLER; EFFECT_MAPPED; FULLY_TESTED | yes | Mapping creado para robo y descarte condicional de energía. |
| xy1-2 | M Venusaur-EX | Pokémon | MEGA, EX | Crisis Vine | none | Mega Evolution rule; Pokémon-EX rule | `Your opponent's Active Pokémon is now Paralyzed and Poisoned.` | DAMAGE_PLUS_STATUS; APPLY_STATUS | MEDIUM | yes | ApplySpecialConditionEffectHandler | no | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_SUPPORTED_BY_GENERIC_HANDLER; EFFECT_MAPPED; FULLY_TESTED | yes | Fase 11E.1 mapea ambas condiciones. La regla Mega Evolution no se ejecuta como efecto de ataque. |
| xy1-5 | Beedrill | Pokémon | Stage 2 | Poison Jab; Flash Needle | none | none | Poison Jab aplica Poisoned. Flash Needle hace daño variable por moneda y prevención futura. | DAMAGE_PLUS_STATUS; DAMAGE_PLUS_COIN_FLIP; PREVENT_DAMAGE | HIGH | partial | ApplySpecialConditionEffectHandler partial | yes | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_MAPPED; REQUIRES_CUSTOM_HANDLER; NOT_IMPLEMENTED_YET | no | Poison Jab mapeado/testeado; Flash Needle queda pendiente. |
| xy1-6 | Ledyba | Pokémon | Basic | Spinning Attack | none | none | Daño puro. | DAMAGE_ONLY | LOW | yes | AttackService | no | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_SUPPORTED_BY_GENERIC_HANDLER; EFFECT_MAPPED; FULLY_TESTED | yes | Mapping vacío agregado para daño puro. |
| xy1-8 | Volbeat | Pokémon | Basic | Luring Glow; Signal Beam | none | none | Luring Glow cambia Activo rival. Signal Beam aplica Confused. | SWITCH_ACTIVE; DAMAGE_PLUS_STATUS | MEDIUM | partial | ApplySpecialConditionEffectHandler partial; SwitchActiveEffectHandler available | no/tbd | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_MAPPED; NOT_IMPLEMENTED_YET | no | Signal Beam mapeado/testeado; Luring Glow queda para pase posterior de switch. |
| xy1-15 | Scatterbug | Pokémon | Basic | Bug Bite | none | none | Daño puro. | DAMAGE_ONLY | LOW | yes | AttackService | no | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_SUPPORTED_BY_GENERIC_HANDLER; EFFECT_MAPPED; FULLY_TESTED | yes | Mapping vacío agregado para daño puro. |
| xy1-18 | Skiddo | Pokémon | Basic | Lead; Tackle | none | none | Lead busca Supporter en mazo. Tackle es daño puro. | SEARCH_DECK; DAMAGE_ONLY | MEDIUM | partial | AttackService partial; SearchDeckEffectHandler available | no/tbd | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_MAPPED; NOT_IMPLEMENTED_YET | no | Tackle mapeado/testeado; Lead queda pendiente por selección/reveal/shuffle. |
| xy1-20 | Slugma | Pokémon | Basic | Flamethrower | none | none | `Discard an Energy attached to this Pokémon.` | DISCARD_ENERGY | LOW | yes | DiscardAttachedEnergyEffectHandler | no | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_SUPPORTED_BY_GENERIC_HANDLER; EFFECT_MAPPED; FULLY_TESTED | yes | Mapea descarte de una Energía propia unida al atacante. |
| xy1-21 | Magcargo | Pokémon | Stage 1 | Magma Mantle; Heat Blast | none | none | Magma Mantle descarta/revisa tope de mazo para modificar daño. Heat Blast es daño puro. | DISCARD_CARD; MODIFY_DAMAGE; DAMAGE_ONLY | MEDIUM | partial | AttackService partial | yes | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_MAPPED; REQUIRES_CUSTOM_HANDLER; NOT_IMPLEMENTED_YET | no | Heat Blast mapeado/testeado; Magma Mantle queda pendiente. |
| xy1-22 | Pansear | Pokémon | Basic | Live Coal; Fireworks | none | none | Live Coal daño puro. Fireworks descarta una Energía unida a este Pokémon. | DAMAGE_ONLY; DISCARD_ENERGY | LOW | yes | AttackService; DiscardAttachedEnergyEffectHandler | no | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_SUPPORTED_BY_GENERIC_HANDLER; EFFECT_MAPPED; FULLY_TESTED | yes | Ambos ataques mapeados/testeados. |
| xy1-24 | Fennekin | Pokémon | Basic | Will-O-Wisp | none | none | Daño puro. | DAMAGE_ONLY | LOW | yes | AttackService | no | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_SUPPORTED_BY_GENERIC_HANDLER; EFFECT_MAPPED; FULLY_TESTED | yes | Mapping vacío agregado para daño puro. |
| xy1-27 | Fletchinder | Pokémon | Stage 1 | Flame Charge; Fire Wing | none | none | Flame Charge busca/adjunta Energía Fire desde mazo. Fire Wing es daño puro. | SEARCH_DECK; ATTACH_ENERGY; DAMAGE_ONLY | MEDIUM | partial | AttackService partial; SearchDeckEffectHandler/AttachEnergyEffectHandler available | no/tbd | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_MAPPED; NOT_IMPLEMENTED_YET | no | Fire Wing mapeado/testeado; Flame Charge queda pendiente por search+attach+shuffle. |
| xy1-29 | Blastoise-EX | Pokémon | Basic, EX | Rapid Spin; Splash Bomb | none | Pokémon-EX rule | Rapid Spin cambia ambos Activos. Splash Bomb: con tails, este Pokémon se hace 30 daño. | SWITCH_ACTIVE; DAMAGE_PLUS_COIN_FLIP | MEDIUM | partial | CoinFlipEffectHandler; DealDamageEffectHandler partial | no/tbd | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_MAPPED; NOT_IMPLEMENTED_YET | no | Splash Bomb mapeado/testeado; Rapid Spin queda pendiente. |
| xy1-31 | Shellder | Pokémon | Basic | Rain Splash | none | none | Daño puro. | DAMAGE_ONLY | LOW | yes | AttackService | no | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_SUPPORTED_BY_GENERIC_HANDLER; EFFECT_MAPPED; FULLY_TESTED | yes | Mapping vacío agregado para daño puro. |
| xy1-32 | Cloyster | Pokémon | Stage 1 | Clamp Crush; Spike Cannon | none | none | Clamp Crush: heads Paralyzed y descarta Energía del Activo rival. Spike Cannon hace daño variable por 5 monedas. | DAMAGE_PLUS_COIN_FLIP; DAMAGE_PLUS_STATUS; DISCARD_ENERGY | MEDIUM | partial | CoinFlipEffectHandler; CompositeEffectHandler; ApplySpecialConditionEffectHandler; DiscardAttachedEnergyEffectHandler | yes/tbd | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_MAPPED; NOT_IMPLEMENTED_YET | no | Clamp Crush mapeado/testeado; Spike Cannon queda pendiente. |
| xy1-123 | Professor's Letter | Trainer | Item | none | none | Item | `Search your deck for up to 2 basic Energy cards, reveal them, and put them into your hand. Shuffle your deck afterward.` | SEARCH_DECK | MEDIUM | partial | SearchDeckEffectHandler; ShuffleDeckEffectHandler | no/tbd | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_SUPPORTED_BY_GENERIC_HANDLER; NOT_IMPLEMENTED_YET | no | Handler genérico disponible, pero falta mapping carta por carta y contrato público de selección/reveal. |
| xy1-127 | Shauna | Trainer | Supporter | none | none | Supporter rule | `Shuffle your hand into your deck. Then, draw 5 cards.` | DRAW_CARDS; DISCARD_CARD; CUSTOM_REQUIRED | MEDIUM | partial | DiscardCardsEffectHandler; ShuffleDeckEffectHandler; DrawCardsEffectHandler | yes/tbd | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_SUPPORTED_BY_GENERIC_HANDLER; NOT_IMPLEMENTED_YET | no | Los handlers base existen parcialmente; falta composición/mapping y modelar mover mano completa al mazo. |
| xy1-14 | Chesnaught | Pokémon | Stage 2 | Touchdown | Spiky Shield | none | Ability: al recibir daño de ataque, pone 3 contadores en el atacante. Touchdown cura 20. | ABILITY_PASSIVE; CONTINUOUS_EFFECT; DAMAGE_PLUS_HEAL | HIGH | partial | HealDamageEffectHandler partial; reactive infra pendiente | yes/tbd | DATA_IMPORTED; EFFECT_CLASSIFIED; EFFECT_MAPPED; REQUIRES_CUSTOM_HANDLER; NOT_IMPLEMENTED_YET | no | Touchdown mapeado/testeado; Spiky Shield queda pendiente. |
| xy1-95 | Slurpuff | Pokémon | Stage 1 | Draining Kiss | Sweet Veil | none | Ability: Pokémon propios con Energía Fairy no pueden ser afectados por condiciones especiales; remueve condiciones. Draining Kiss cura 30. | ABILITY_PASSIVE; CONTINUOUS_EFFECT; DAMAGE_PLUS_HEAL | HIGH | partial | HealDamageEffectHandler partial; SpecialConditionPrevention infra partial | yes/tbd | DATA_IMPORTED; EFFECT_CLASSIFIED; REQUIRES_CUSTOM_HANDLER; NOT_IMPLEMENTED_YET | no | Fase 11D permite prevención genérica, pero falta condición por Energía Fairy, cleanup continuo y mapping/test de carta. |

## Reglas de auditoría

1. No interpretar `effectText`, `rules`, `attacks` ni `abilities` como lógica ejecutable.
2. No marcar cobertura completa de XY1 hasta auditar las 146 cartas y testear efectos implementados.
3. Si un ataque solo hace daño base, puede quedar auditado con mapping vacío y nota `DAMAGE_ONLY`.
4. Si un efecto requiere buscar/revelar desde mazo, documentar privacidad y selección.
5. Si un efecto requiere persistencia temporal, habilidad pasiva, reemplazo forzado, búsqueda, descarte de mano/mazo o modificación de reglas, marcar gap antes de implementar.

## Gaps detectables por la herramienta

Handlers/infraestructura que deja de ser gap base en Fase 11C, aunque todavía requiere mappings y tests por carta:

- `SearchDeckEffectHandler`.
- `ShuffleDeckEffectHandler`.
- `DiscardCardsEffectHandler`.
- `AttachEnergyEffectHandler`.
- `MoveEnergyEffectHandler`.
- `SwitchActiveEffectHandler`.
- `PlaceDamageCountersEffectHandler`.

Infraestructura que deja de ser gap base en Fase 11D, aunque todavía requiere mappings y tests por carta:

- `DamageModifierEffect` / `PreventDamageEffect` vía `ModifierResolver`.
- `RetreatCostModifierEffect` vía `ModifierResolver`.
- Prevención de condiciones especiales vía `StatusEffectManager` contextual.
- Fuentes continuas desde Pokémon, Tool y Stadium.

Gaps todavía pendientes para completar XY1:

- Mappings/tests reales para modificadores de daño, prevención, retiro y condiciones.
- Servicio completo de habilidades activadas con límites de uso.
- Resolver reactivo completo para habilidades pasivas/reactivas.
- Efectos continuos complejos de Tool/Stadium con condiciones/duración avanzadas.
- Selección desde zonas ocultas, reveal y privacidad de mano/mazo/premios.
- Timing avanzado: before damage, after damage, between turns, on damaged, while in play, next turn.

## Aclaración ACE SPEC

El set obligatorio `xy1` no contiene cartas AS TÁCTICO / ACE SPEC. La validación “máximo 1 ACE SPEC por mazo” queda fuera del alcance obligatorio XY1 y solo debería agregarse si se habilitan sets futuros que sí incluyan esa mecánica.
