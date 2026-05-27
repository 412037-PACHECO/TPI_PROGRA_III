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

- Objetivo: CRUD y validación de mazos.
- Entregables: decks, deck cards y validaciones obligatorias para alcance base `xy1`: exactamente 60 cartas, máximo 4 copias por nombre salvo excepciones oficiales aplicables, y al menos 1 Pokémon Básico.
- Dependencias: catálogo.
- Riesgos: reglas de nombre Pokémon mal interpretadas; implementar por error AS TÁCTICO / ACE SPEC como validación obligatoria aunque `xy1` no contiene esa mecánica.
- Criterio: mazo válido/inválido detectado con mensajes claros.

Nota: la validación "máximo 1 AS TÁCTICO / ACE SPEC por mazo" no aplica a mazos solo `xy1`. Queda como validación condicional/futura si se incorporan sets opcionales que incluyan cartas ACE SPEC.

## Fase 4 - Modelo de Game State

- Objetivo: estado completo en memoria y serializable.
- Entregables: Game, PlayerState, zonas, cartas instancia, turn state.
- Dependencias: catálogo/decks.
- Riesgos: zonas duplicadas o estado incompleto.
- Criterio: snapshot reconstruible en tests.

## Fase 5 - Setup y mulligan

- Objetivo: preparación oficial.
- Entregables: barajar, robar 7, mulligan, activo/banca, premios.
- Dependencias: Game State.
- Riesgos: timing de mulligan.
- Criterio: tests de ambos/single mulligan y setup válido.

## Fase 6 - Turnos y acciones básicas

- Objetivo: DRAW/MAIN/ATTACK/BETWEEN_TURNS y comandos básicos.
- Entregables: TurnManager, flags, acciones de banca/energía/retiro básico.
- Dependencias: setup.
- Riesgos: flags mal reseteados.
- Criterio: tests de fase/turno/jugador.

## Fase 7 - Ataques base

- Objetivo: declarar ataque, validar energía y daño simple.
- Entregables: AttackResolver, DamageCalculator inicial.
- Dependencias: turnos.
- Riesgos: mezclar efectos complejos demasiado pronto.
- Criterio: ataque simple con daño/debilidad/resistencia testeado.

## Fase 8 - Knockout, premios y victoria

- Objetivo: resolver KO y condiciones de finalización.
- Entregables: KnockoutResolver, PrizeResolver, VictoryConditionChecker.
- Dependencias: ataques.
- Riesgos: simultaneidades.
- Criterio: premios, EX y victoria cubiertos por tests.

## Fase 9 - Condiciones especiales

- Objetivo: implementar estados XY1.
- Entregables: StatusEffectManager y between turns.
- Dependencias: turnos/KO.
- Riesgos: timing y limpieza.
- Criterio: tests para las 5 condiciones.

## Fase 10 - Motor de efectos

- Objetivo: soportar efectos reales XY1 incrementalmente.
- Entregables: EffectRegistry, handlers genéricos/custom.
- Dependencias: auditoría XY1.
- Riesgos: hardcode desordenado.
- Criterio: cada efecto implementado está auditado y testeado.

## Fase 11 - Persistencia de snapshots/logs

- Objetivo: guardar/reconstruir partida.
- Entregables: snapshots versionados, log inmutable.
- Dependencias: Game State estable.
- Riesgos: pérdida de información oculta.
- Criterio: reconstrucción exacta en tests.

## Fase 12 - WebSockets

- Objetivo: sincronización realtime.
- Entregables: canales, eventos, reconexión, vistas seguras.
- Dependencias: persistencia y eventos.
- Riesgos: filtración o duplicados.
- Criterio: contrato WS testeado.

## Fase 13 - Tests fuertes

- Objetivo: cobertura y casos críticos.
- Entregables: unit/integration/WS/E2E mínimos.
- Dependencias: fases previas.
- Riesgos: tests frágiles.
- Criterio: JaCoCo >=80% y críticos >90%.

## Fase 14 - Preparación para frontend

- Objetivo: contratos estables para Angular.
- Entregables: OpenAPI, DTOs, eventos, vistas seguras.
- Dependencias: backend estable.
- Riesgos: cambiar contrato tarde.
- Criterio: frontend puede consumir comandos/vistas sin duplicar reglas.
