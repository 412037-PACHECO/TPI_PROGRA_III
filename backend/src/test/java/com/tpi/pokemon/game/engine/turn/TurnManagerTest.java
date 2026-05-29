package com.tpi.pokemon.game.engine.turn;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.enums.TurnPhase;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.CardDefinitionRef;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.DiscardPile;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PrizeCards;
import com.tpi.pokemon.game.domain.model.TurnState;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.CardDrawSkippedEvent;
import com.tpi.pokemon.game.engine.event.CardDrawnEvent;
import com.tpi.pokemon.game.engine.event.DeckOutLossDetectedEvent;
import com.tpi.pokemon.game.engine.event.GameFinishedEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.MainPhaseStartedEvent;
import com.tpi.pokemon.game.engine.event.VictoryDetectedEvent;
import com.tpi.pokemon.game.engine.victory.FinishReason;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TurnManagerTest {
    private static final GameId GAME_ID = new GameId("turn-test-game");
    private static final PlayerId PLAYER_ONE = new PlayerId("player-one");
    private static final PlayerId PLAYER_TWO = new PlayerId("player-two");
    private static final CardDefinitionRef BASIC = new CardDefinitionRef("basic", "Basic", CardSupertype.POKEMON, Set.of(CardSubtype.BASIC));

    private final TurnManager turnManager = new TurnManager();

    @Test
    void startingPlayerSkipsDrawOnTheirFirstTurnAndEntersMainPhase() {
        GameState state = activeGame(
                player(PLAYER_ONE, List.of(card("p1-deck-1", PLAYER_ONE)), List.of(card("p1-hand-1", PLAYER_ONE)), 0),
                player(PLAYER_TWO, List.of(card("p2-deck-1", PLAYER_TWO)), List.of(card("p2-hand-1", PLAYER_TWO)), 0),
                TurnState.preparedForFirstTurn(PLAYER_ONE)
        );

        GameState updated = turnManager.startTurn(state, new StartTurnCommand(PLAYER_ONE));

        assertThat(updated.getTurnState().phase()).isEqualTo(TurnPhase.MAIN);
        assertThat(updated.getTurnState().turnNumber()).isEqualTo(1);
        assertThat(updated.getTurnState().cardDrawnThisTurn()).isFalse();
        assertThat(updated.getPlayerOneState().getHand().getCards()).hasSize(1);
        assertThat(updated.getPlayerOneState().getDeck().getCards()).hasSize(1);
        assertThat(eventsOfType(updated, CardDrawSkippedEvent.class)).hasSize(1);
    }

    @Test
    void secondPlayerDrawsNormallyAfterFirstPlayerEndsTurn() {
        CardInstance drawn = card("p2-deck-1", PLAYER_TWO);
        GameState started = turnManager.startTurn(activeGame(
                player(PLAYER_ONE, List.of(card("p1-deck-1", PLAYER_ONE)), List.of(card("p1-hand-1", PLAYER_ONE)), 0),
                player(PLAYER_TWO, List.of(drawn, card("p2-deck-2", PLAYER_TWO)), List.of(card("p2-hand-1", PLAYER_TWO)), 0),
                TurnState.preparedForFirstTurn(PLAYER_ONE)
        ), new StartTurnCommand(PLAYER_ONE));

        GameState ended = turnManager.endTurn(started, new EndTurnCommand(PLAYER_ONE));
        GameState secondTurn = turnManager.startTurn(ended, new StartTurnCommand(PLAYER_TWO));

        assertThat(secondTurn.getTurnState().currentPlayer()).isEqualTo(PLAYER_TWO);
        assertThat(secondTurn.getTurnState().phase()).isEqualTo(TurnPhase.MAIN);
        assertThat(secondTurn.getTurnState().turnNumber()).isEqualTo(2);
        assertThat(secondTurn.getPlayerTwoState().getHand().getCards()).extracting(CardInstance::id).contains(drawn.id());
        assertThat(secondTurn.getPlayerTwoState().getHand().getCards()).hasSize(2);
        assertThat(secondTurn.getPlayerTwoState().getDeck().getCards()).hasSize(1);
        assertThat(eventsOfType(secondTurn, CardDrawnEvent.class)).hasSize(1);
    }

    @Test
    void deckOutFinishesGameWhenCurrentPlayerMustDrawFromEmptyDeck() {
        GameState state = activeGame(
                player(PLAYER_ONE, List.of(), List.of(card("p1-hand-1", PLAYER_ONE)), 1),
                player(PLAYER_TWO, List.of(card("p2-deck-1", PLAYER_TWO)), List.of(card("p2-hand-1", PLAYER_TWO)), 0),
                new TurnState(PLAYER_ONE, PLAYER_TWO, 1, TurnPhase.NOT_STARTED, false, false, false, false, false)
        );

        GameState updated = turnManager.startTurn(state, new StartTurnCommand(PLAYER_ONE));

        assertThat(updated.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(updated.getTurnState().phase()).isEqualTo(TurnPhase.DRAW);
        assertThat(updated.getFinishResult()).hasValueSatisfying(result -> {
            assertThat(result.winnerId()).isEqualTo(PLAYER_TWO);
            assertThat(result.loserId()).isEqualTo(PLAYER_ONE);
            assertThat(result.reasons()).containsExactly(FinishReason.DECK_OUT);
        });
        assertThat(eventsOfType(updated, DeckOutLossDetectedEvent.class)).hasSize(1);
        assertThat(eventsOfType(updated, VictoryDetectedEvent.class)).hasSize(1);
        assertThat(eventsOfType(updated, GameFinishedEvent.class)).hasSize(1);
        assertThat(eventsOfType(updated, MainPhaseStartedEvent.class)).isEmpty();
    }

    @Test
    void endTurnResetsOncePerTurnFlagsAndNextStartIncrementsTurnNumber() {
        GameState state = activeGame(
                player(PLAYER_ONE, List.of(card("p1-deck-1", PLAYER_ONE)), List.of(card("p1-hand-1", PLAYER_ONE)), 1),
                player(PLAYER_TWO, List.of(card("p2-deck-1", PLAYER_TWO)), List.of(card("p2-hand-1", PLAYER_TWO)), 0),
                new TurnState(PLAYER_ONE, PLAYER_ONE, 1, TurnPhase.MAIN, false, true, true, true, true)
        );

        GameState ended = turnManager.endTurn(state, new EndTurnCommand(PLAYER_ONE));
        GameState next = turnManager.startTurn(ended, new StartTurnCommand(PLAYER_TWO));

        assertThat(ended.getTurnState().currentPlayer()).isEqualTo(PLAYER_TWO);
        assertThat(ended.getTurnState().phase()).isEqualTo(TurnPhase.NOT_STARTED);
        assertThat(ended.getTurnState().energyAttachedThisTurn()).isFalse();
        assertThat(ended.getTurnState().supporterPlayedThisTurn()).isFalse();
        assertThat(ended.getTurnState().stadiumPlayedThisTurn()).isFalse();
        assertThat(ended.getTurnState().retreatedThisTurn()).isFalse();
        assertThat(next.getTurnState().turnNumber()).isEqualTo(2);
        assertThat(next.getTurnState().phase()).isEqualTo(TurnPhase.MAIN);
    }

    private GameState activeGame(PlayerGameState playerOne, PlayerGameState playerTwo, TurnState turnState) {
        return new GameState(GAME_ID, GameStatus.ACTIVE, playerOne, playerTwo, turnState, List.of());
    }

    private PlayerGameState player(PlayerId playerId, List<CardInstance> deck, List<CardInstance> hand, int turnsTaken) {
        return new PlayerGameState(playerId, new DeckZone(deck), new HandZone(hand), PrizeCards.empty(), DiscardPile.empty(), BoardState.empty(), turnsTaken);
    }

    private CardInstance card(String id, PlayerId owner) {
        return new CardInstance(new CardInstanceId(id), BASIC, owner);
    }

    private <T extends GameEvent> List<T> eventsOfType(GameState state, Class<T> type) {
        return state.getEvents().stream().filter(type::isInstance).map(type::cast).toList();
    }
}
