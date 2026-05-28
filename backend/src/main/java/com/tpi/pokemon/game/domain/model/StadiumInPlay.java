package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record StadiumInPlay(CardInstance card, PlayerId playedBy, int playedTurnNumber) {
    public StadiumInPlay {
        Objects.requireNonNull(card, "card must not be null");
        Objects.requireNonNull(playedBy, "playedBy must not be null");
        if (playedTurnNumber < 0) {
            throw new IllegalArgumentException("playedTurnNumber must not be negative");
        }
    }
}
