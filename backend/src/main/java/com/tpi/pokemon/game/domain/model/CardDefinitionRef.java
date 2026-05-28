package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import java.util.Objects;
import java.util.Set;

public record CardDefinitionRef(
        String cardId,
        String name,
        CardSupertype supertype,
        Set<CardSubtype> subtypes,
        String evolvesFrom,
        Integer retreatCost
) {
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
        if (retreatCost != null && retreatCost < 0) {
            throw new IllegalArgumentException("retreatCost must not be negative");
        }
        subtypes = Set.copyOf(subtypes);
        if (evolvesFrom != null && evolvesFrom.isBlank()) {
            evolvesFrom = null;
        }
    }

    public CardDefinitionRef(String cardId, String name) {
        this(cardId, name, CardSupertype.TRAINER, Set.of(), null, null);
    }

    public CardDefinitionRef(String cardId, String name, CardSupertype supertype, Set<CardSubtype> subtypes) {
        this(cardId, name, supertype, subtypes, null, null);
    }

    public boolean isPokemon() {
        return supertype == CardSupertype.POKEMON;
    }

    public boolean isEnergy() {
        return supertype == CardSupertype.ENERGY;
    }

    public boolean isTrainer() {
        return supertype == CardSupertype.TRAINER;
    }

    public boolean isItem() {
        return isTrainer() && subtypes.contains(CardSubtype.ITEM);
    }

    public boolean isSupporter() {
        return isTrainer() && subtypes.contains(CardSubtype.SUPPORTER);
    }

    public boolean isStadium() {
        return isTrainer() && subtypes.contains(CardSubtype.STADIUM);
    }

    public boolean isTool() {
        return isTrainer() && subtypes.contains(CardSubtype.TOOL);
    }

    public boolean isBasicPokemon() {
        return isPokemon() && subtypes.contains(CardSubtype.BASIC);
    }

    public boolean canEvolve() {
        return isPokemon() && evolvesFrom != null;
    }

    public boolean hasKnownRetreatCost() {
        return retreatCost != null;
    }
}
