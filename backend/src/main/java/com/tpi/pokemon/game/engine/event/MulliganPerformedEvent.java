package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import java.util.List;
import java.util.Objects;

public record MulliganPerformedEvent(GameId gameId, PlayerId playerId, int mulliganNumber, List<CardInstanceId> revealedCardIds) implements GameEvent {
    public MulliganPerformedEvent {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(revealedCardIds, "revealedCardIds must not be null");
        if (mulliganNumber < 1) {
            throw new IllegalArgumentException("mulliganNumber must be positive");
        }
        if (revealedCardIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("revealedCardIds must not contain null values");
        }
        revealedCardIds = List.copyOf(revealedCardIds);
    }
}
