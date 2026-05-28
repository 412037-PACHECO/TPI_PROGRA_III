package com.tpi.pokemon.game.engine.action;

import java.util.Objects;

public record PokemonTarget(PokemonTargetZone zone, int benchIndex) {
    public PokemonTarget {
        Objects.requireNonNull(zone, "zone must not be null");
        if (zone == PokemonTargetZone.BENCH && benchIndex < 0) {
            throw new IllegalArgumentException("benchIndex must not be negative for bench targets");
        }
    }

    public static PokemonTarget active() {
        return new PokemonTarget(PokemonTargetZone.ACTIVE, -1);
    }

    public static PokemonTarget bench(int benchIndex) {
        return new PokemonTarget(PokemonTargetZone.BENCH, benchIndex);
    }
}
