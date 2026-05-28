# 00 - Overview

## Resumen ejecutivo

Este proyecto corresponde al TPI de Programación III: una aplicación cliente-servidor para jugar Pokémon TCG basada en el reglamento oficial XY1. El sistema debe permitir que dos jugadores construyan mazos, inicien una partida, ejecuten acciones válidas, sincronicen el estado en tiempo real y finalicen la partida bajo condiciones oficiales de victoria/derrota.

Estado actual del repositorio: documentación base creada y backend Spring Boot implementado incrementalmente hasta Fase 6: catálogo/cache local XY1, Deck Builder backend, modelo interno base de Game State, setup/mulligan inicial y motor de turnos con acciones MAIN estructurales. No existe todavía frontend Angular implementado. Ataques, daño, knockout, condiciones especiales, WebSocket y efectos ejecutables siguen pendientes.

## Objetivo del sistema

Construir una versión digital funcional de Pokémon TCG con:

- Backend Java 21 + Spring Boot 3.x como fuente de verdad.
- Frontend futuro Angular 21+ como capa de presentación.
- Persistencia relacional en PostgreSQL o MySQL.
- Comunicación en tiempo real vía WebSockets.
- Catálogo de cartas desde pokemontcg.io v2, con set base obligatorio `xy1`.
- Testing con JUnit, Mockito y JaCoCo.

## Alcance obligatorio

- Reglas de juego RF-01: setup, mulligan, turnos, ataques, knockout, condiciones especiales y victoria/derrota.
- Tipos de cartas RF-02: Pokémon, Pokémon-EX, Energías y Entrenadores, incluyendo subtipos aplicables en XY1 como Objeto, Partidario, Estadio y Herramienta. AS TÁCTICO / ACE SPEC no forma parte del set obligatorio `xy1`; solo aplica si el equipo incorpora expansiones opcionales que lo incluyan.
- Gestión de partida RF-03: estados `WAITING`, `SETUP`, `ACTIVE`, `FINISHED`.
- Deck Builder RF-04 con validaciones oficiales y set `xy1`.
- Persistencia completa RF-05 después de cada acción relevante.
- WebSockets RF-06 para sincronización en tiempo real.
- UI RF-07 en fase futura.
- RNF: rendimiento, calidad, testing, patrones, seguridad y usabilidad.

## Alcance opcional

- Megaevolución.
- Chat durante partida.
- Ranking o historial de partidas.
- Animaciones frontend.
- Expansiones adicionales además de `xy1`.
- Validaciones adicionales propias de expansiones opcionales, por ejemplo máximo 1 AS TÁCTICO / ACE SPEC si se agrega un set que contenga esa mecánica.
- JWT, si el equipo decide incorporarlo.

## Principio central

El backend es la única fuente de verdad. El frontend futuro solo enviará comandos y mostrará vistas seguras del estado. Ninguna regla oficial del juego debe decidirse en Angular, WebSocket handlers o controllers REST.

## Fuentes base

- `TUP_3C_PIII_TPI_POKEMON_TCG.pdf`.
- `xy1-rulebook-es.pdf`.
- API pública: https://docs.pokemontcg.io
