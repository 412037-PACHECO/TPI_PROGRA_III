package com.tpi.pokemon.game.domain.model;

import static com.tpi.pokemon.game.GameStateFixtures.GAME_ID;
import static com.tpi.pokemon.game.GameStateFixtures.PLAYER_ONE;
import static com.tpi.pokemon.game.GameStateFixtures.PLAYER_TWO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.enums.TurnPhase;
import com.tpi.pokemon.game.engine.event.GameCreatedEvent;
import org.junit.jupiter.api.Test;

class GameStateTest {
    @Test
    void createdBuildsBaseStateForTwoDistinctPlayersAndEmitsGameCreatedEvent() {
        GameState state = GameState.created(GAME_ID, PLAYER_ONE, PLAYER_TWO);

        assertThat(state.getGameId()).isEqualTo(GAME_ID);
        assertThat(state.getStatus()).isEqualTo(GameStatus.CREATED);
        assertThat(state.getPlayerOneState().getPlayerId()).isEqualTo(PLAYER_ONE);
        assertThat(state.getPlayerTwoState().getPlayerId()).isEqualTo(PLAYER_TWO);
        assertThat(state.getTurnState()).isEqualTo(TurnState.notStarted());
        assertThat(state.getTurnState().phase()).isEqualTo(TurnPhase.NOT_STARTED);
        assertThat(state.getEvents())
                .singleElement()
                .isInstanceOfSatisfying(GameCreatedEvent.class, event -> {
                    assertThat(event.gameId()).isEqualTo(GAME_ID);
                    assertThat(event.playerOneId()).isEqualTo(PLAYER_ONE);
                    assertThat(event.playerTwoId()).isEqualTo(PLAYER_TWO);
                });
    }

    @Test
    void createdRejectsSamePlayerOnBothSides() {
        assertThatThrownBy(() -> GameState.created(GAME_ID, PLAYER_ONE, PLAYER_ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distinct");
    }
}
