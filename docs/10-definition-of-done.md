# 10 - Definition of Done

## Backend

- Código claro, mantenible y consistente con Java 21/Spring Boot 3.x.
- Validaciones de entrada y errores controlados.
- No hay reglas de juego en controllers ni handlers WebSocket.
- Persistencia actualizada cuando corresponde.
- Tests mínimos del caso de uso.
- Documentación actualizada si cambia arquitectura o contrato.
- No implementa validaciones de expansiones opcionales como obligatorias del alcance base; por ejemplo, AS TÁCTICO / ACE SPEC no se valida para mazos solo `xy1`.

## Game Engine

- Implementa la regla oficial o documenta explícitamente cualquier decisión pendiente.
- Es independiente de Spring, JPA, WebSocket y API externa.
- Recibe comandos, valida, muta estado y emite eventos.
- Protege invariantes de zonas, turnos, fases, energía, KO y privacidad.
- Tiene unit tests válidos, inválidos y borde.

## Carta / efecto

- La carta está registrada en la matriz XY1.
- Su categoría de efecto está identificada.
- Usa handler genérico cuando sea posible.
- Handler custom documenta motivo y alcance.
- Tiene tests de comportamiento.
- No filtra información privada por eventos/logs.
- Si pertenece a una mecánica no presente en `xy1` como AS TÁCTICO / ACE SPEC, queda fuera del DoD base y requiere decisión explícita de expansión opcional, auditoría y tests propios.

## Endpoint

- Tiene request/response definido.
- Valida entrada básica.
- Delega a application service.
- No contiene lógica de reglas.
- Devuelve errores accionables.
- Tiene test de integración si toca persistencia o contrato relevante.

## Prueba

- Valida comportamiento real, no implementación accidental.
- Tiene fixtures reproducibles.
- No depende de orden global ni aleatoriedad no controlada.
- Si prueba dominio, no levanta Spring innecesariamente.
- Cubre al menos caso válido, inválido y borde para reglas críticas.

## Fase

- Entregables completados.
- Dependencias satisfechas.
- Riesgos nuevos documentados.
- Tests mínimos ejecutables definidos/implementados.
- Documentación actualizada.
- No rompe fases anteriores.

## Cobertura mínima

- JaCoCo global >= 80% antes de entrega.
- `RuleValidator`, `DamageCalculator`, `StatusEffectManager` > 90%.
- Prioridad de cobertura en `TurnManager`, `KnockoutResolver`, `VictoryConditionChecker`.
