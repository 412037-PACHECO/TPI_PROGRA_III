package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class BoardState {
    private final ActivePokemon activePokemon;
    private final Bench bench;
    private final StadiumInPlay activeStadium;

    public BoardState(ActivePokemon activePokemon, Bench bench) {
        this(activePokemon, bench, null);
    }

    public BoardState(ActivePokemon activePokemon, Bench bench, StadiumInPlay activeStadium) {
        this.activePokemon = activePokemon;
        this.bench = Objects.requireNonNull(bench, "bench must not be null");
        this.activeStadium = activeStadium;
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

    public Optional<StadiumInPlay> getActiveStadium() {
        return Optional.ofNullable(activeStadium);
    }

    private void validateNoDuplicateCardsBetweenActiveAndBench() {
        Set<CardInstanceId> seen = new HashSet<>();
        if (activePokemon != null) {
            activePokemon.getPokemon().getEvolutionStack().forEach(card -> addUnique(seen, card));
            for (CardInstance attached : activePokemon.getPokemon().getAttachedCards().getCards()) {
                addUnique(seen, attached);
            }
        }
        for (PokemonInPlay pokemonInPlay : bench.getPokemon()) {
            pokemonInPlay.getEvolutionStack().forEach(card -> addUnique(seen, card));
            for (CardInstance attached : pokemonInPlay.getAttachedCards().getCards()) {
                addUnique(seen, attached);
            }
        }
        if (activeStadium != null) {
            addUnique(seen, activeStadium.card());
        }
    }

    private void addUnique(Set<CardInstanceId> seen, CardInstance card) {
        if (!seen.add(card.id())) {
            throw new IllegalArgumentException("BoardState must not contain duplicate card instance: " + card.id().value());
        }
    }
}
