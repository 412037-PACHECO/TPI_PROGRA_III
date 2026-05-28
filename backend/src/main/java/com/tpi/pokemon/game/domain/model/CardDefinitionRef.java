package com.tpi.pokemon.game.domain.model;

public record CardDefinitionRef(String cardId, String name) {
    public CardDefinitionRef {
        if (cardId == null || cardId.isBlank()) {
            throw new IllegalArgumentException("Card definition id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Card definition name must not be blank");
        }
    }
}
