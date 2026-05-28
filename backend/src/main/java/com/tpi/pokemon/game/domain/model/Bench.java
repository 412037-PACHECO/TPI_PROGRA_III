package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class Bench {
    public static final int MAX_SIZE = 5;

    private final List<PokemonInPlay> pokemon;

    public Bench(List<PokemonInPlay> pokemon) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        if (pokemon.size() > MAX_SIZE) {
            throw new IllegalArgumentException("Bench cannot contain more than " + MAX_SIZE + " Pokemon");
        }
        if (pokemon.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Bench must not contain null Pokemon");
        }
        Set<CardInstanceId> seen = new HashSet<>();
        for (PokemonInPlay pokemonInPlay : pokemon) {
            pokemonInPlay.getEvolutionStack().forEach(card -> addUnique(seen, card));
            for (CardInstance attached : pokemonInPlay.getAttachedCards().getCards()) {
                addUnique(seen, attached);
            }
        }
        this.pokemon = List.copyOf(pokemon);
    }

    public static Bench empty() {
        return new Bench(List.of());
    }

    public List<PokemonInPlay> getPokemon() {
        return pokemon;
    }

    private void addUnique(Set<CardInstanceId> seen, CardInstance card) {
        if (!seen.add(card.id())) {
            throw new IllegalArgumentException("Bench must not contain duplicate card instance: " + card.id().value());
        }
    }
}
