# 11 - XY1 Audit Matrix

## Objetivo

Preparar la auditoría del set obligatorio `xy1` antes de implementar efectos carta por carta. No se inventa una auditoría completa hasta importar o consultar formalmente las cartas.

Estado Fase 10: matriz preparada para trazabilidad. No representa cobertura completa de XY1 ni debe leerse como listado exhaustivo de efectos implementados.

Handlers genéricos disponibles para mapear filas auditadas:

- `DealDamageEffectHandler`.
- `HealDamageEffectHandler`.
- `ApplySpecialConditionEffectHandler`.
- `DrawCardsEffectHandler`.
- `DiscardAttachedEnergyEffectHandler`.
- `CoinFlipEffectHandler`.
- `CompositeEffectHandler`.

La existencia de un handler no implica que una carta XY1 esté soportada: cada carta requiere fila auditada y tests.

## Aclaración ACE SPEC

El set obligatorio `xy1` no contiene cartas AS TÁCTICO / ACE SPEC. En consecuencia, la matriz XY1 no debe exigir implementación ni testing de la validación "máximo 1 AS TÁCTICO / ACE SPEC por mazo". Esa regla queda documentada como condicional para expansiones opcionales futuras que sí incluyan esa mecánica.

| Tema | Aplicabilidad XY1 | Decisión |
|---|---|---|
| AS TÁCTICO / ACE SPEC | No aplica; `xy1` no contiene cartas ACE SPEC | No validar máximo 1 por mazo en alcance obligatorio. Implementar solo si se agregan sets opcionales con ACE SPEC. |

## Columnas oficiales

| cardId | name | supertype | subtypes | source | effectText | effectCategory | complexity | genericHandlers | customHandlerRequired | auditStatus | implemented | tested | notes |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| _pendiente_ | _pendiente_ | _pendiente_ | _pendiente_ | attack/ability/rule/trainer | _pendiente_ | _pendiente_ | _pendiente_ | _pendiente_ | _pendiente_ | not_audited | no | no | Completar tras importación/revisión formal de XY1 |

## Categorías sugeridas

- `DAMAGE`
- `SPECIAL_CONDITION`
- `HEALING`
- `DRAW`
- `SEARCH`
- `DISCARD`
- `ENERGY_ATTACHMENT`
- `ENERGY_DISCARD`
- `SWITCH`
- `STADIUM`
- `TOOL`
- `ABILITY`
- `PERSISTENT_MODIFIER`
- `CUSTOM`

## Complejidad sugerida

- `LOW`: efecto directo y genérico.
- `MEDIUM`: requiere targets, condiciones o timing específico.
- `HIGH`: modifica reglas, tiene persistencia temporal o interacción compleja.
- `CUSTOM`: requiere handler específico.

## Reglas de auditoría

1. Primero clasificar todas las cartas XY1.
2. Luego implementar handlers genéricos reutilizables.
3. Recién después implementar handlers custom.
4. Una carta no se considera terminada si no está `implemented=yes` y `tested=yes`.
5. Si el texto requiere revelar/buscar cartas, documentar impacto de privacidad.
6. No inferir implementación desde texto natural: `effectText` es evidencia, no lógica ejecutable.
7. Si un ataque solo hace daño base ya soportado por `AttackService`, marcar handler como `NoOpEffect` o `DealDamageEffect` según corresponda y aclararlo en notas.

## Estados de auditoría

- `not_audited`: fila placeholder o carta sin revisar.
- `audited`: carta/efecto clasificado, implementación pendiente.
- `implemented`: handler implementado, cobertura de test insuficiente o pendiente de confirmar.
- `tested`: implementación y tests existentes; efecto soportado para el alcance declarado.

## Trazabilidad mínima por fila

Cada fila completa debe permitir responder:

- Qué texto/campo del catálogo motivó el efecto.
- Qué categoría genérica o handler custom lo resuelve.
- Qué timing aplica: ataque, acción MAIN, entre turnos, continuo o disparado.
- Qué tests prueban mutación de estado, eventos, targets y privacidad.
