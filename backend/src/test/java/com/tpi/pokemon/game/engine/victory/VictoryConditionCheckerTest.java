package com.tpi.pokemon.game.engine.victory;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.enums.TurnPhase;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.DiscardPile;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PrizeCards;
import com.tpi.pokemon.game.domain.model.TurnState;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.List;
import org.junit.jupiter.api.Test;

class VictoryConditionCheckerTest {
    private static final GameId GAME_ID = new GameId("victory-test-game");
    private static final PlayerId PLAYER_ONE = new PlayerId("player-one");
    private static final PlayerId PLAYER_TWO = new PlayerId("player-two");

    private final VictoryConditionChecker checker = new VictoryConditionChecker();

    @Test
    void lastPrizeTakenWinsGame() {
        GameState state = game(player(PLAYER_ONE, PrizeCards.empty(), BoardState.empty()), player(PLAYER_TWO, PrizeCards.empty(), BoardState.empty()));

        GameFinishResult result = checker.checkAfterKnockout(state, PLAYER_ONE, PLAYER_TWO).orElseThrow();

        assertThat(result.type()).isEqualTo(GameFinishType.SINGLE_WINNER);
        assertThat(result.winnerId()).isEqualTo(PLAYER_ONE);
        assertThat(result.reasons()).contains(FinishReason.PRIZES_TAKEN);
    }

    @Test
    void deckOutMakesOpponentWinner() {
        GameFinishResult result = checker.deckOut(PLAYER_ONE, PLAYER_TWO);

        assertThat(result.winnerId()).isEqualTo(PLAYER_TWO);
        assertThat(result.loserId()).isEqualTo(PLAYER_ONE);
        assertThat(result.reasons()).containsExactly(FinishReason.DECK_OUT);
    }

    @Test
    void simultaneousVictoryCanBeRepresentedAsSuddenDeathRequired() {
        GameFinishResult result = checker.suddenDeathRequired(List.of(FinishReason.SIMULTANEOUS_WIN, FinishReason.SUDDEN_DEATH_REQUIRED));

        assertThat(result.type()).isEqualTo(GameFinishType.SUDDEN_DEATH_REQUIRED);
        assertThat(result.winner()).isEmpty();
        assertThat(result.reasons()).contains(FinishReason.SIMULTANEOUS_WIN, FinishReason.SUDDEN_DEATH_REQUIRED);
    }

    private GameState game(PlayerGameState playerOne, PlayerGameState playerTwo) {
        return new GameState(GAME_ID, GameStatus.ACTIVE, playerOne, playerTwo, new TurnState(PLAYER_ONE, PLAYER_ONE, 2, TurnPhase.ATTACK, false, false, false, false, false), List.of());
    }

    private PlayerGameState player(PlayerId playerId, PrizeCards prizes, BoardState board) {
        return new PlayerGameState(playerId, DeckZone.empty(), HandZone.empty(), prizes, DiscardPile.empty(), board, 1);
    }
}
