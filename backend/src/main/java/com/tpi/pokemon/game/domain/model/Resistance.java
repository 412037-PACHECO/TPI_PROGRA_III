package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.enums.PokemonType;
import java.util.Objects;

public record Resistance(PokemonType type, int reduction) {
    public Resistance {
        Objects.requireNonNull(type, "type must not be null");
        if (reduction < 0 || reduction % 10 != 0) {
            throw new IllegalArgumentException("reduction must be non-negative and a multiple of 10");
        }
    }
}
