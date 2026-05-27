# 11 - XY1 Audit Matrix

## Objetivo

Preparar la auditoría del set obligatorio `xy1` antes de implementar efectos carta por carta. No se inventa una auditoría completa hasta importar o consultar formalmente las cartas.

## Aclaración ACE SPEC

El set obligatorio `xy1` no contiene cartas AS TÁCTICO / ACE SPEC. En consecuencia, la matriz XY1 no debe exigir implementación ni testing de la validación "máximo 1 AS TÁCTICO / ACE SPEC por mazo". Esa regla queda documentada como condicional para expansiones opcionales futuras que sí incluyan esa mecánica.

| Tema | Aplicabilidad XY1 | Decisión |
|---|---|---|
| AS TÁCTICO / ACE SPEC | No aplica; `xy1` no contiene cartas ACE SPEC | No validar máximo 1 por mazo en alcance obligatorio. Implementar solo si se agregan sets opcionales con ACE SPEC. |

## Columnas oficiales

| cardId | name | supertype | subtypes | attacks | abilities | rules | effectText | effectCategory | complexity | genericHandlers | customHandlerRequired | implemented | tested | notes |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| _pendiente_ | _pendiente_ | _pendiente_ | _pendiente_ | _pendiente_ | _pendiente_ | _pendiente_ | _pendiente_ | _pendiente_ | _pendiente_ | _pendiente_ | _pendiente_ | no | no | Completar tras importación XY1 |

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
