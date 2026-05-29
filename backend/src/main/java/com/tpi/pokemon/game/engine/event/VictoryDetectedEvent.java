package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.victory.FinishReason;

public record VictoryDetectedEvent(GameId gameId, PlayerId winnerId, PlayerId loserId, FinishReason reason) implements GameEvent {}
