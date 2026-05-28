package com.tpi.pokemon.game.domain.model;

import java.util.Objects;

public final class ActivePokemon {
    private final PokemonInPlay pokemon;

    public ActivePokemon(PokemonInPlay pokemon) {
        this.pokemon = Objects.requireNonNull(pokemon, "pokemon must not be null");
    }

    public PokemonInPlay getPokemon() {
        return pokemon;
    }
}
