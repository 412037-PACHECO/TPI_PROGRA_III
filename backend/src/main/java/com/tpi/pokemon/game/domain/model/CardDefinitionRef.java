package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.enums.PokemonType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record CardDefinitionRef(
        String cardId,
        String name,
        CardSupertype supertype,
        Set<CardSubtype> subtypes,
        String evolvesFrom,
        Integer retreatCost,
        Integer hp,
        List<PokemonType> pokemonTypes,
        List<AttackDefinition> attacks,
        List<Weakness> weaknesses,
        List<Resistance> resistances,
        EnergyProfile energyProfile
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
        if (hp != null && (hp <= 0 || hp % 10 != 0)) {
            throw new IllegalArgumentException("hp must be positive and a multiple of 10 when provided");
        }
        Objects.requireNonNull(pokemonTypes, "pokemonTypes must not be null");
        Objects.requireNonNull(attacks, "attacks must not be null");
        Objects.requireNonNull(weaknesses, "weaknesses must not be null");
        Objects.requireNonNull(resistances, "resistances must not be null");
        Objects.requireNonNull(energyProfile, "energyProfile must not be null");
        if (pokemonTypes.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("pokemonTypes must not contain null values");
        }
        if (attacks.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("attacks must not contain null values");
        }
        if (weaknesses.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("weaknesses must not contain null values");
        }
        if (resistances.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("resistances must not contain null values");
        }
        subtypes = Set.copyOf(subtypes);
        pokemonTypes = List.copyOf(pokemonTypes);
        attacks = List.copyOf(attacks);
        weaknesses = List.copyOf(weaknesses);
        resistances = List.copyOf(resistances);
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

    public CardDefinitionRef(String cardId, String name, CardSupertype supertype, Set<CardSubtype> subtypes, String evolvesFrom, Integer retreatCost) {
        this(cardId, name, supertype, subtypes, evolvesFrom, retreatCost, null, List.of(), List.of(), List.of(), List.of(), EnergyProfile.none());
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

    public Optional<AttackDefinition> attackById(String attackId) {
        if (attackId == null || attackId.isBlank()) {
            return Optional.empty();
        }
        return attacks.stream().filter(attack -> attack.attackId().equals(attackId)).findFirst();
    }

    public boolean providesEnergy() {
        return isEnergy() && !energyProfile.provides().isEmpty();
    }
}
