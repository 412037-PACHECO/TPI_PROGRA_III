package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.GameId;
import java.util.Objects;

public record SetupCompletedEvent(GameId gameId) implements GameEvent {
    public SetupCompletedEvent {
        Objects.requireNonNull(gameId, "gameId must not be null");
    }
}
