# 05 - Card Effects Strategy

## Objetivo

Soportar efectos reales de cartas XY1 sin convertir el motor en una colección desordenada de `if cardId == ...`.

## CardDefinition vs CardInstance

- `CardDefinition`: datos estáticos de pokemontcg.io/cache local. Incluye nombre, set, supertype, subtypes, ataques, habilidades, reglas, HP, debilidad, resistencia, coste de retirada y texto.
- `CardInstance`: copia concreta en una partida. Tiene ID único, zona, dueño, estado y vínculo con otros objetos de partida.

## EffectDefinition vs EffectExecution

- `EffectDefinition`: describe qué efecto tiene una carta o ataque de forma declarativa o semideclarativa.
- `EffectExecution`: ejecución contextual del efecto sobre un `GameState`, con jugador, targets, fase, aleatoriedad controlada y eventos resultantes.

## EffectRegistry

Registro interno que asocia cartas/ataques/habilidades con handlers.

Responsabilidades:

- Resolver handlers genéricos por categoría.
- Resolver handlers específicos cuando un efecto no entra en modelo genérico.
- Hacer explícito qué cartas XY1 están soportadas, testeadas o pendientes.

## EffectHandler

Contrato conceptual:

```text
canHandle(effectDefinition, context)
validate(effectDefinition, context)
execute(effectDefinition, context) -> events + state changes
```

Los handlers no deben depender de Spring, JPA, WebSocket ni API externa.

## Efectos genéricos

- `DealDamageEffect`
- `PlaceDamageCountersEffect`
- `ApplySpecialConditionEffect`
- `HealDamageEffect`
- `DrawCardsEffect`
- `SearchDeckEffect`
- `ShuffleDeckEffect`
- `DiscardCardsEffect`
- `DiscardEnergyEffect`
- `AttachEnergyEffect`
- `SwitchActiveEffect`
- `RetreatCostModifierEffect`
- `DamageModifierEffect`
- `PreventDamageEffect`
- `PersistentEffect`

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

## Matriz de auditoría XY1

La auditoría se mantiene en `docs/11-xy1-audit-matrix.md`. Debe indicar complejidad, categoría, handlers requeridos, estado de implementación y tests.
