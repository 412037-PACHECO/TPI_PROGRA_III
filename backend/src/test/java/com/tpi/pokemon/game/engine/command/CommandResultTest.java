package com.tpi.pokemon.game.engine.command;

import static com.tpi.pokemon.game.GameStateFixtures.GAME_ID;
import static com.tpi.pokemon.game.GameStateFixtures.PLAYER_ONE;
import static com.tpi.pokemon.game.GameStateFixtures.PLAYER_TWO;
import static org.assertj.core.api.Assertions.assertThat;

import com.tpi.pokemon.game.domain.model.GameState;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommandResultTest {
    @Test
    void successContainsGameStateAndEvents() {
        GameState gameState = GameState.created(GAME_ID, PLAYER_ONE, PLAYER_TWO);

        CommandResult result = CommandResult.success(gameState, gameState.getEvents());

        assertThat(result.success()).isTrue();
        assertThat(result.gameState()).isSameAs(gameState);
        assertThat(result.events()).containsExactlyElementsOf(gameState.getEvents());
        assertThat(result.error()).isNull();
    }

    @Test
    void failureContainsErrorAndNoEvents() {
        CommandResult result = CommandResult.failure("invalid command");

        assertThat(result.success()).isFalse();
        assertThat(result.gameState()).isNull();
        assertThat(result.events()).isEqualTo(List.of());
        assertThat(result.error()).isEqualTo("invalid command");
    }
}
