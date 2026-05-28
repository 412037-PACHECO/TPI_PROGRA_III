package com.tpi.pokemon.game.domain.model;

import static com.tpi.pokemon.game.GameStateFixtures.PLAYER_ONE;
import static com.tpi.pokemon.game.GameStateFixtures.PLAYER_TWO;
import static com.tpi.pokemon.game.GameStateFixtures.card;
import static com.tpi.pokemon.game.GameStateFixtures.pokemon;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerGameStateTest {
    @Test
    void rejectsSameCardInstanceAcrossTwoZones() {
        CardInstance duplicated = card("duplicated-card", PLAYER_ONE);

        assertThatThrownBy(() -> new PlayerGameState(
                PLAYER_ONE,
                new DeckZone(List.of(duplicated)),
                new HandZone(List.of(duplicated)),
                PrizeCards.empty(),
                DiscardPile.empty(),
                BoardState.empty()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate card instance");
    }

    @Test
    void rejectsCardsOwnedByAnotherPlayer() {
        CardInstance opponentCard = card("opponent-card", PLAYER_TWO);

        assertThatThrownBy(() -> new PlayerGameState(
                PLAYER_ONE,
                new DeckZone(List.of(opponentCard)),
                HandZone.empty(),
                PrizeCards.empty(),
                DiscardPile.empty(),
                BoardState.empty()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not owned by player");
    }

    @Test
    void rejectsOpponentOwnedPokemonOnBoard() {
        assertThatThrownBy(() -> new PlayerGameState(
                PLAYER_ONE,
                DeckZone.empty(),
                HandZone.empty(),
                PrizeCards.empty(),
                DiscardPile.empty(),
                new BoardState(new ActivePokemon(pokemon("opponent-active", PLAYER_TWO)), Bench.empty())
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not owned by player");
    }
}
