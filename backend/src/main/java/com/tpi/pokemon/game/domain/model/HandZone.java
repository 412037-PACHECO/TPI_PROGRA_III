package com.tpi.pokemon.game.domain.model;

import java.util.List;

public final class HandZone {
    private final List<CardInstance> cards;

    public HandZone(List<CardInstance> cards) {
        GameStateValidation.requireUniqueCards(cards, "hand");
        this.cards = List.copyOf(cards);
    }

    public static HandZone empty() {
        return new HandZone(List.of());
    }

    public List<CardInstance> getCards() {
        return cards;
    }
}
