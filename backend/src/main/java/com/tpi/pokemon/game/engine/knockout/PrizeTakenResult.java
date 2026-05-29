package com.tpi.pokemon.game.engine.knockout;

import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.List;
import java.util.Objects;

public record PrizeTakenResult(PlayerId playerId, List<CardInstance> takenCards, int remainingPrizeCount) {
    public PrizeTakenResult {
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(takenCards, "takenCards must not be null");
        if (remainingPrizeCount < 0) {
            throw new IllegalArgumentException("remainingPrizeCount must not be negative");
        }
        takenCards = List.copyOf(takenCards);
    }
}
