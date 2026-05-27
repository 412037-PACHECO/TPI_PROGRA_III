# 08 - Testing Strategy

## Principio

Las reglas del juego se prueban principalmente en el Game Engine puro, sin Spring, DB ni WebSocket. Si una regla puede testearse en dominio, no debe depender de integración.

## Pirámide de pruebas

1. Muchos unit tests de Game Engine.
2. Tests de integración de casos de uso backend.
3. Tests de persistencia para mapeos/snapshots/versionado.
4. Tests WebSocket de contrato, privacidad y reconexión.
5. E2E frontend mínimo cuando exista Angular.

## Unit tests prioritarios

### RuleValidator

- Setup válido/inválido.
- Mulligan requerido.
- Acción por jugador incorrecto.
- Acción fuera de fase.
- Energía más de una vez por turno.
- Ataque sin energía suficiente.
- Retiro sin coste suficiente.
- Evolución fuera de regla.

### DamageCalculator

- Daño base.
- Debilidad x2.
- Resistencia -20 mínimo 0.
- Modificadores antes/después de debilidad/resistencia.
- Daño cero.
- KO exacto y overkill.

### StatusEffectManager

- Dormido impide atacar/retirarse.
- Paralizado impide atacar/retirarse y se limpia correctamente.
- Confundido puede cancelar ataque y aplicar 3 contadores.
- Envenenado aplica 1 contador entre turnos.
- Quemado aplica moneda y 2 contadores si corresponde.
- Retiro/evolución limpian condiciones.
- Incompatibilidad Dormido/Confundido/Paralizado.

### TurnManager

- Inicio/cambio de turno.
- Reset de flags.
- Primer turno sin robo/ataque para jugador inicial según TPI.
- Ataque termina turno.

### KnockoutResolver

- KO de activo.
- KO de banca.
- Descarte de cartas unidas.
- Toma de premios normal y Pokémon-EX.
- Promoción obligatoria.
- KO múltiple/simultáneo.

### VictoryConditionChecker

- Último premio.
- Rival sin Pokémon en juego.
- Mazo vacío al robar al inicio.
- Victoria simultánea y Muerte Súbita.

## Integration tests

- Crear partida.
- Unirse a partida.
- Setup completo.
- Mulligan múltiple.
- Ejecutar turno básico.
- Ataque → daño → knockout → premio.
- Victoria por premios.
- Persistir y recuperar snapshot.

## Tests de persistencia

- Mantener orden de mazo.
- Mantener cartas ocultas.
- Guardar manos, premios, descarte, activo, banca, cartas unidas, daño, condiciones y flags.
- Versionar snapshots.

## Tests WebSocket

- Conexión a partida.
- Evento tras acción válida.
- Error controlado tras acción inválida.
- Reconexión con vista actual.
- No filtrar mano rival, premios ocultos ni orden de mazo.
- Rechazo de acción duplicada o versión vieja.

## Cobertura objetivo

- JaCoCo global: >= 80%.
- Módulos críticos: > 90% en `RuleValidator`, `DamageCalculator`, `StatusEffectManager`.
- Cobertura útil: cada regla crítica debe tener caso válido, inválido y borde.

## Fixtures de partida

- `basicGameReadyToAttack`.
- `weaknessScenario`.
- `resistanceScenario`.
- `knockoutScenario`.
- `victoryByPrizes`.
- `statusPoisonBetweenTurns`.
- `mulliganRequired`.

Preferir builders legibles antes que JSON gigantes.
