package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;

public record MainPhaseStartedEvent(GameId gameId, PlayerId playerId, int turnNumber) implements GameEvent {}
