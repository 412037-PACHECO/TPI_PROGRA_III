package com.tpi.pokemon.game.domain.model;

import java.util.List;

public final class DiscardPile {
    private final List<CardInstance> cards;

    public DiscardPile(List<CardInstance> cards) {
        GameStateValidation.requireUniqueCards(cards, "discard pile");
        this.cards = List.copyOf(cards);
    }

    public static DiscardPile empty() {
        return new DiscardPile(List.of());
    }

    public List<CardInstance> getCards() {
        return cards;
    }
}
