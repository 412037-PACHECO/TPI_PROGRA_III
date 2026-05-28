package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;

public record CardDrawSkippedEvent(GameId gameId, PlayerId playerId, int turnNumber, String reason) implements GameEvent {}
