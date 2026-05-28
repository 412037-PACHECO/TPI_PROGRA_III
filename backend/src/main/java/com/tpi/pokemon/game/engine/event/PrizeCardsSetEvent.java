package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.List;
import java.util.Objects;

public record PrizeCardsSetEvent(GameId gameId, PlayerId playerId, List<CardInstanceId> cardIds) implements GameEvent {
    public PrizeCardsSetEvent {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(cardIds, "cardIds must not be null");
        if (cardIds.size() != 6) {
            throw new IllegalArgumentException("Prize cards event must contain 6 card ids");
        }
        if (cardIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("cardIds must not contain null values");
        }
        cardIds = List.copyOf(cardIds);
    }
}
