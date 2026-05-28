package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.enums.PokemonType;
import java.util.Objects;

public record Weakness(PokemonType type, int multiplier) {
    public Weakness {
        Objects.requireNonNull(type, "type must not be null");
        if (multiplier < 1) {
            throw new IllegalArgumentException("multiplier must be at least 1");
        }
    }
}
