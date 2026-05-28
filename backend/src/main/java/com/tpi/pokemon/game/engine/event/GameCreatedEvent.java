package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record GameCreatedEvent(GameId gameId, PlayerId playerOneId, PlayerId playerTwoId) implements GameEvent {
    public GameCreatedEvent {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(playerOneId, "playerOneId must not be null");
        Objects.requireNonNull(playerTwoId, "playerTwoId must not be null");
    }
}
