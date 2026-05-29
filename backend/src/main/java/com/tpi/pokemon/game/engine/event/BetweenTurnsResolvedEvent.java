package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.GameId;

public record BetweenTurnsResolvedEvent(GameId gameId, int turnNumber) implements GameEvent {}
