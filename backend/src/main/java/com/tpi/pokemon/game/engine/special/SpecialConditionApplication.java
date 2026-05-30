package com.tpi.pokemon.game.engine.special;

import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.engine.effect.modifier.AppliedModifier;
import java.util.List;
import java.util.Objects;

public record SpecialConditionApplication(PokemonInPlay pokemon, boolean prevented, List<AppliedModifier> appliedModifiers) {
    public SpecialConditionApplication {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        Objects.requireNonNull(appliedModifiers, "appliedModifiers must not be null");
        if (appliedModifiers.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("appliedModifiers must not contain null values");
        }
        appliedModifiers = List.copyOf(appliedModifiers);
    }
}
