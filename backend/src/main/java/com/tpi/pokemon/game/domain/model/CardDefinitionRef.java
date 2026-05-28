package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import java.util.Objects;
import java.util.Set;

public record CardDefinitionRef(String cardId, String name, CardSupertype supertype, Set<CardSubtype> subtypes) {
    public CardDefinitionRef {
        if (cardId == null || cardId.isBlank()) {
            throw new IllegalArgumentException("Card definition id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Card definition name must not be blank");
        }
        Objects.requireNonNull(supertype, "supertype must not be null");
        Objects.requireNonNull(subtypes, "subtypes must not be null");
        if (subtypes.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("subtypes must not contain null values");
        }
        subtypes = Set.copyOf(subtypes);
    }

    public CardDefinitionRef(String cardId, String name) {
        this(cardId, name, CardSupertype.TRAINER, Set.of());
    }

    public boolean isBasicPokemon() {
        return supertype == CardSupertype.POKEMON && subtypes.contains(CardSubtype.BASIC);
    }
}
