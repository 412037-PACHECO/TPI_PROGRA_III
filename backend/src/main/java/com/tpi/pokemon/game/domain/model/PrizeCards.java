package com.tpi.pokemon.game.domain.model;

import java.util.List;

public final class PrizeCards {
    private final List<CardInstance> cards;

    public PrizeCards(List<CardInstance> cards) {
        GameStateValidation.requireUniqueCards(cards, "prize cards");
        if (cards.size() != 0 && cards.size() != 1 && cards.size() != 6) {
            throw new IllegalArgumentException("PrizeCards size must be 0, 1, or 6");
        }
        this.cards = List.copyOf(cards);
    }

    public static PrizeCards empty() {
        return new PrizeCards(List.of());
    }

    public List<CardInstance> getCards() {
        return cards;
    }
}
