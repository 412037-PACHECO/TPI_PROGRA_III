package com.tpi.pokemon.game.application.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.TurnState;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.persistence.infrastructure.GameActionLogEntity;
import com.tpi.pokemon.game.persistence.infrastructure.GameSessionEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameLogProjectionServiceTest {
    @Test
    void publicLogDoesNotExposeRawJsonPayloads() {
        PlayerId playerOne = new PlayerId("player-one");
        PlayerId playerTwo = new PlayerId("player-two");
        GameState state = new GameState(new GameId("log-game"), GameStatus.CREATED, PlayerGameState.empty(playerOne), PlayerGameState.empty(playerTwo), TurnState.notStarted(), List.of());
        GameActionLogEntity raw = GameActionLogEntity.create(GameSessionEntity.create("log-game"), "log-game", 1, 0, "NOT_STARTED", "player-one", "PRIVATE_ACTION", "{\"secretCardId\":\"p2-hand-1\"}", "{\"private\":true}", "[{\"hidden\":true}]");

        List<GameLogPublicView> projected = new GameLogProjectionService().project(state, playerOne, List.of(raw));

        assertThat(projected).hasSize(1);
        assertThat(projected.get(0).actionType()).isEqualTo("PRIVATE_ACTION");
        assertThat(projected.get(0).summary()).doesNotContain("secretCardId", "p2-hand-1", "private", "hidden");
        assertThat(projected.get(0).publicEvents()).containsExactly("PRIVATE_ACTION");
    }
}
