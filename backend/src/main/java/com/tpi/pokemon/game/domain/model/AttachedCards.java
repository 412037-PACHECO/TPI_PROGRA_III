package com.tpi.pokemon.game.domain.model;

import java.util.List;

public final class AttachedCards {
    private final List<CardInstance> cards;

    public AttachedCards(List<CardInstance> cards) {
        GameStateValidation.requireUniqueCards(cards, "attached cards");
        this.cards = List.copyOf(cards);
    }

    public static AttachedCards empty() {
        return new AttachedCards(List.of());
    }

    public List<CardInstance> getCards() {
        return cards;
    }
}
