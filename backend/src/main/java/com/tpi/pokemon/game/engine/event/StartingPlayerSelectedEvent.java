package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record StartingPlayerSelectedEvent(GameId gameId, PlayerId playerId) implements GameEvent {
    public StartingPlayerSelectedEvent {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(playerId, "playerId must not be null");
    }
}
