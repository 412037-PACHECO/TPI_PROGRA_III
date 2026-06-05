# 10 - Definition of Done

## Backend

- Código claro, mantenible y consistente con Java 21/Spring Boot 3.x.
- Validaciones de entrada y errores controlados.
- No hay reglas de juego en controllers ni handlers WebSocket.
- Persistencia actualizada cuando corresponde.
- Tests mínimos del caso de uso.
- Documentación actualizada si cambia arquitectura o contrato.
- No implementa validaciones de expansiones opcionales como obligatorias del alcance base; por ejemplo, AS TÁCTICO / ACE SPEC no se valida para mazos solo `xy1`.
- Deck Builder puede persistir mazos incompletos; las reglas de mazo completo deben estar en endpoint/servicio explícito de validación, no como bloqueo del CRUD.

## Persistencia de partida

- Persiste metadata consultable de partida sin parsear el snapshot para listados básicos.
- Guarda snapshot JSON completo y versionado de `GameState`, incluyendo zonas ocultas necesarias para reconstrucción.
- Mantiene log inmutable append-only con `sequence` monotónica por partida.
- Snapshot, log y metadata se actualizan de forma consistente después de cada acción relevante aceptada.
- Reconstruye desde el último snapshot y aplica/verifica logs posteriores en orden.
- No acopla el engine a entidades JPA, repositorios, DTOs REST ni WebSocket.
- No expone `GameState` crudo ni logs privados como vista pública de frontend.
- Documenta `snapshotVersion`/versión de formato del snapshot y estrategia ante cambios de formato.
- Tiene tests de reconstrucción, duplicados/fuera de orden, privacidad de datos y consistencia de metadata.

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
- Si expone datos de partida, distingue metadata/log interno de vistas seguras finales por jugador.
- No publica `GameState` completo como contrato frontend porque contiene zonas ocultas.

## Vista segura de partida

- Requiere `viewerPlayerId` y valida que pertenezca a la partida.
- El jugador ve su mano completa, descarte y campo propio.
- El jugador no ve orden de mazo ni premios boca abajo salvo efecto futuro explícito.
- El rival solo muestra conteos de mano, mazo y premios.
- El campo público del rival muestra Activo, Banca, daño, condiciones y attachments públicos.
- El descarte de ambos jugadores es visible.
- Las selecciones pendientes solo muestran candidatos privados al jugador autorizado.
- El log público no incluye `commandJson`, `resultJson` ni `eventsJson` crudos.
- Tiene tests que fallarían ante filtración de mano rival, orden de mazo, premios ocultos o candidatos privados.

## WebSocket / Realtime

- Configura endpoint STOMP `/ws` y broker simple para `/topic` y `/queue`.
- Usa canales públicos por partida solo para eventos sin zonas ocultas.
- Usa canales privados por jugador/perspectiva para `GameViewResponse` y logs públicos.
- Reutiliza las mismas vistas seguras de REST; no duplica lógica de privacidad.
- No publica `GameState`, snapshots JSON ni logs crudos.
- Publica eventos de sesión/reconexión después de create/join/reconnect.
- La reconexión devuelve vista segura actualizada.
- Tiene tests con `SimpMessagingTemplate` mockeado que verifican destinos y payloads seguros.

## Gameplay API

- Cada endpoint representa un comando real existente del engine.
- Carga el último snapshot antes de ejecutar.
- Valida jugador y delega reglas al engine.
- Persiste log + snapshot solo si la acción fue válida.
- Devuelve `GameViewResponse`, nunca `GameState`.
- Una acción inválida devuelve error claro y no crea snapshot falso.
- No expone endpoints falsos para reglas sin contrato seguro, como pending selections o Trainers complejos.
- Documenta gaps explícitos.

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
