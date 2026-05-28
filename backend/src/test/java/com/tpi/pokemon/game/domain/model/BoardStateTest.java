package com.tpi.pokemon.game.domain.model;

import static com.tpi.pokemon.game.GameStateFixtures.pokemon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class BoardStateTest {
    @Test
    void acceptsOneActiveAndBenchWithDifferentCardInstances() {
        BoardState board = new BoardState(
                new ActivePokemon(pokemon("active-1")),
                new Bench(List.of(pokemon("bench-1")))
        );

        assertThat(board.getActivePokemon()).isPresent();
        assertThat(board.getBench().getPokemon()).hasSize(1);
    }

    @Test
    void rejectsSameCardInstanceInActiveAndBench() {
        PokemonInPlay samePokemon = pokemon("shared-card");

        assertThatThrownBy(() -> new BoardState(
                new ActivePokemon(samePokemon),
                new Bench(List.of(samePokemon))
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate card instance");
    }
}
