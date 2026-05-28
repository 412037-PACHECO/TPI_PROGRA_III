package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record DeckShuffledEvent(GameId gameId, PlayerId playerId, int cardCount) implements GameEvent {
    public DeckShuffledEvent {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(playerId, "playerId must not be null");
        if (cardCount < 0) {
            throw new IllegalArgumentException("cardCount must not be negative");
        }
    }
}
