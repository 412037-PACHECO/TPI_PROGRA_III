package com.tpi.pokemon.game.domain.model;

import static com.tpi.pokemon.game.GameStateFixtures.pokemon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class BenchTest {
    @Test
    void acceptsUpToFivePokemon() {
        Bench bench = new Bench(List.of(
                pokemon("p-1"),
                pokemon("p-2"),
                pokemon("p-3"),
                pokemon("p-4"),
                pokemon("p-5")
        ));

        assertThat(bench.getPokemon()).hasSize(5);
    }

    @Test
    void rejectsMoreThanFivePokemon() {
        assertThatThrownBy(() -> new Bench(List.of(
                pokemon("p-1"),
                pokemon("p-2"),
                pokemon("p-3"),
                pokemon("p-4"),
                pokemon("p-5"),
                pokemon("p-6")
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("more than 5");
    }
}
