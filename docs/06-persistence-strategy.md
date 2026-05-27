# 06 - Persistence Strategy

## Objetivo

Persistir suficiente estado para reconstruir una partida completa ante falla, reinicio o reconexión.

## Qué se guarda normalizado

- Jugadores.
- Mazos guardados y cartas de mazo.
- Catálogo local de cartas XY1.
- Metadata de partidas: jugadores, estado, timestamps, ganador.
- Logs/eventos de partida como registros inmutables.

## Qué se guarda como snapshot

El estado interno completo de la partida después de cada acción relevante. Inicialmente se recomienda snapshot JSON versionado para evitar una normalización prematura de todas las zonas y efectos.

Tradeoff:

- Ventaja: simple, flexible y permite iterar reglas.
- Riesgo: queries complejas sobre estado interno son más difíciles.

## Estado completo requerido para reconstrucción

- Manos de ambos jugadores.
- Mazos con orden exacto.
- Cartas de Premio con orden/ocultamiento.
- Pilas de descarte.
- Pokémon Activo de cada jugador.
- Banca de cada jugador.
- Cartas unidas: energías, herramientas y evoluciones.
- Daño/contadores de cada Pokémon en juego.
- Condiciones especiales activas.
- Estadio activo.
- Flags del turno: energía unida, retiro usado, Partidario usado, Estadio usado, primer turno, etc.
- Fase actual y jugador activo.
- Efectos persistentes pendientes.
- Log/eventos asociados.

## Log inmutable

Cada entrada debe incluir:

- `matchId`.
- Versión/secuencia.
- Turno y fase.
- Jugador origen.
- Tipo de acción/evento.
- Resultado.
- Timestamp.
- Datos públicos o privados clasificados correctamente.

El log sirve para auditoría, debugging, reconstrucción parcial y soporte a reconexión. No reemplaza al snapshot completo.

## Persistencia después de acciones relevantes

Persistir snapshot + log después de:

- Crear/unirse a partida.
- Resolver mulligan/setup.
- Robar carta.
- Jugar carta.
- Adjuntar energía.
- Evolucionar.
- Retirar.
- Usar habilidad.
- Declarar/resolver ataque.
- Resolver entre turnos.
- KO/premios/victoria.

## Versionado de snapshots

Cada snapshot debe tener versión incremental para:

- Detectar acciones duplicadas o fuera de orden.
- Resolver reconexión.
- Evitar sobrescritura por concurrencia.
