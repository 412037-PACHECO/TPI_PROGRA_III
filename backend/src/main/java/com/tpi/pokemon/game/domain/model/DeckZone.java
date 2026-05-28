package com.tpi.pokemon.game.domain.model;

import java.util.List;

public final class DeckZone {
    private final List<CardInstance> cards;

    public DeckZone(List<CardInstance> cards) {
        GameStateValidation.requireUniqueCards(cards, "deck");
        this.cards = List.copyOf(cards);
    }

    public static DeckZone empty() {
        return new DeckZone(List.of());
    }

    public List<CardInstance> getCards() {
        return cards;
    }
}
