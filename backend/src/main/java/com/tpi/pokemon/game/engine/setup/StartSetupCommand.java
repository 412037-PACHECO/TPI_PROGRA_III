package com.tpi.pokemon.game.engine.setup;

import com.tpi.pokemon.game.domain.model.CardInstance;
import java.util.List;
import java.util.Objects;

public record StartSetupCommand(List<CardInstance> playerOneDeck, List<CardInstance> playerTwoDeck) {
    public StartSetupCommand {
        Objects.requireNonNull(playerOneDeck, "playerOneDeck must not be null");
        Objects.requireNonNull(playerTwoDeck, "playerTwoDeck must not be null");
        if (playerOneDeck.stream().anyMatch(Objects::isNull) || playerTwoDeck.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("decks must not contain null cards");
        }
        playerOneDeck = List.copyOf(playerOneDeck);
        playerTwoDeck = List.copyOf(playerTwoDeck);
    }
}
