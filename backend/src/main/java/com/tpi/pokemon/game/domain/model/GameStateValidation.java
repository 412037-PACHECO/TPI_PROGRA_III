package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

final class GameStateValidation {
    private GameStateValidation() {
    }

    static void requireNoNullCards(Collection<CardInstance> cards, String fieldName) {
        Objects.requireNonNull(cards, fieldName + " must not be null");
        if (cards.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(fieldName + " must not contain null cards");
        }
    }

    static void requireUniqueCards(Collection<CardInstance> cards, String fieldName) {
        requireNoNullCards(cards, fieldName);
        Set<CardInstanceId> ids = new HashSet<>();
        for (CardInstance card : cards) {
            if (!ids.add(card.id())) {
                throw new IllegalArgumentException(fieldName + " must not contain duplicate CardInstanceId: " + card.id().value());
            }
        }
    }
}
