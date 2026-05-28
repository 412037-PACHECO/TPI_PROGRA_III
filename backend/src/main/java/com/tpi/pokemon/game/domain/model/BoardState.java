package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class BoardState {
    private final ActivePokemon activePokemon;
    private final Bench bench;

    public BoardState(ActivePokemon activePokemon, Bench bench) {
        this.activePokemon = activePokemon;
        this.bench = Objects.requireNonNull(bench, "bench must not be null");
        validateNoDuplicateCardsBetweenActiveAndBench();
    }

    public static BoardState empty() {
        return new BoardState(null, Bench.empty());
    }

    public Optional<ActivePokemon> getActivePokemon() {
        return Optional.ofNullable(activePokemon);
    }

    public Bench getBench() {
        return bench;
    }

    private void validateNoDuplicateCardsBetweenActiveAndBench() {
        Set<CardInstanceId> seen = new HashSet<>();
        if (activePokemon != null) {
            addUnique(seen, activePokemon.getPokemon().getBaseCard());
            for (CardInstance attached : activePokemon.getPokemon().getAttachedCards().getCards()) {
                addUnique(seen, attached);
            }
        }
        for (PokemonInPlay pokemonInPlay : bench.getPokemon()) {
            addUnique(seen, pokemonInPlay.getBaseCard());
            for (CardInstance attached : pokemonInPlay.getAttachedCards().getCards()) {
                addUnique(seen, attached);
            }
        }
    }

    private void addUnique(Set<CardInstanceId> seen, CardInstance card) {
        if (!seen.add(card.id())) {
            throw new IllegalArgumentException("BoardState must not contain duplicate card instance: " + card.id().value());
        }
    }
}
