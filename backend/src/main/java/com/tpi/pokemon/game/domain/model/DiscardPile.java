package com.tpi.pokemon.game.domain.model;

import java.util.ArrayList;
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

    public DiscardPile withCardsAdded(List<CardInstance> cardsToAdd) {
        List<CardInstance> updated = new ArrayList<>(cards);
        updated.addAll(cardsToAdd);
        return new DiscardPile(updated);
    }
}
