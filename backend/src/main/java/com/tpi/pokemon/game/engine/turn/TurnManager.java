package com.tpi.pokemon.game.engine.turn;

import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.enums.TurnPhase;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.TurnState;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.CardDrawSkippedEvent;
import com.tpi.pokemon.game.engine.event.CardDrawnEvent;
import com.tpi.pokemon.game.engine.event.DeckOutLossDetectedEvent;
import com.tpi.pokemon.game.engine.event.GameFinishedEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.MainPhaseStartedEvent;
import com.tpi.pokemon.game.engine.event.TurnEndedEvent;
import com.tpi.pokemon.game.engine.event.TurnStartedEvent;
import com.tpi.pokemon.game.engine.event.VictoryDetectedEvent;
import com.tpi.pokemon.game.engine.special.BetweenTurnsService;
import com.tpi.pokemon.game.engine.victory.FinishReason;
import com.tpi.pokemon.game.engine.victory.GameFinishResult;
import com.tpi.pokemon.game.engine.victory.VictoryConditionChecker;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TurnManager {
    private final VictoryConditionChecker victoryConditionChecker = new VictoryConditionChecker();
    private final BetweenTurnsService betweenTurnsService;

    public TurnManager() {
        this(new BetweenTurnsService());
    }

    public TurnManager(BetweenTurnsService betweenTurnsService) {
        this.betweenTurnsService = Objects.requireNonNull(betweenTurnsService, "betweenTurnsService must not be null");
    }

    public GameState startTurn(GameState state, StartTurnCommand command) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(command, "command must not be null");
        requireActive(state);
        TurnState turn = state.getTurnState();
        if (turn.phase() != TurnPhase.NOT_STARTED) {
            throw new TurnException("Turn can only start from NOT_STARTED phase");
        }
        if (!command.playerId().equals(turn.currentPlayer())) {
            throw new TurnException("Only the current player can start their turn");
        }

        int nextTurnNumber = turn.turnNumber() + 1;
        TurnState drawTurn = turn.startDrawPhase(nextTurnNumber);
        PlayerGameState currentPlayer = getPlayerState(state, command.playerId()).withTurnsTaken(getPlayerState(state, command.playerId()).getTurnsTaken() + 1);
        List<GameEvent> events = new ArrayList<>(state.getEvents());
        events.add(new TurnStartedEvent(state.getGameId(), command.playerId(), nextTurnNumber));

        if (command.playerId().equals(turn.startingPlayer()) && nextTurnNumber == 1) {
            events.add(new CardDrawSkippedEvent(state.getGameId(), command.playerId(), nextTurnNumber, "Starting player skips first draw"));
            events.add(new MainPhaseStartedEvent(state.getGameId(), command.playerId(), nextTurnNumber));
            return withUpdatedPlayer(state, currentPlayer, GameStatus.ACTIVE, drawTurn.enterMain(false), events);
        }

        if (currentPlayer.getDeck().getCards().isEmpty()) {
            PlayerId winnerId = opponentOf(state, command.playerId());
            GameFinishResult finishResult = victoryConditionChecker.deckOut(command.playerId(), winnerId);
            events.add(new DeckOutLossDetectedEvent(state.getGameId(), command.playerId(), nextTurnNumber));
            events.add(new VictoryDetectedEvent(state.getGameId(), winnerId, command.playerId(), FinishReason.DECK_OUT));
            events.add(new GameFinishedEvent(state.getGameId(), winnerId, command.playerId(), FinishReason.DECK_OUT));
            return withUpdatedPlayer(state, currentPlayer, GameStatus.FINISHED, drawTurn, finishResult, events);
        }

        CardInstance drawn = currentPlayer.getDeck().getCards().get(0);
        List<CardInstance> remainingDeck = currentPlayer.getDeck().getCards().subList(1, currentPlayer.getDeck().getCards().size());
        List<CardInstance> updatedHand = new ArrayList<>(currentPlayer.getHand().getCards());
        updatedHand.add(drawn);
        PlayerGameState updatedPlayer = new PlayerGameState(
                currentPlayer.getPlayerId(),
                new DeckZone(remainingDeck),
                new HandZone(updatedHand),
                currentPlayer.getPrizeCards(),
                currentPlayer.getDiscardPile(),
                currentPlayer.getBoard(),
                currentPlayer.getTurnsTaken()
        );
        events.add(new CardDrawnEvent(state.getGameId(), command.playerId(), drawn.id()));
        events.add(new MainPhaseStartedEvent(state.getGameId(), command.playerId(), nextTurnNumber));
        return withUpdatedPlayer(state, updatedPlayer, GameStatus.ACTIVE, drawTurn.enterMain(true), events);
    }

    public GameState endTurn(GameState state, EndTurnCommand command) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(command, "command must not be null");
        requireActive(state);
        TurnState turn = state.getTurnState();
        if (turn.phase() != TurnPhase.MAIN && turn.phase() != TurnPhase.ATTACK) {
            throw new TurnException("Turn can only end from MAIN or ATTACK phase");
        }
        if (state.getPendingActiveReplacement().isPresent()) {
            throw new TurnException("Turn cannot end while active Pokemon replacement is pending");
        }
        if (!command.playerId().equals(turn.currentPlayer())) {
            throw new TurnException("Only the current player can end their turn");
        }
        List<GameEvent> events = new ArrayList<>(state.getEvents());
        events.add(new TurnEndedEvent(state.getGameId(), command.playerId(), turn.turnNumber()));
        GameState ended = new GameState(state.getGameId(), GameStatus.ACTIVE, state.getPlayerOneState(), state.getPlayerTwoState(), turn, state.getActiveStadium().orElse(null), events);
        GameState afterBetweenTurns = betweenTurnsService.resolveBetweenTurns(ended, events);
        if (afterBetweenTurns.getStatus() != GameStatus.ACTIVE || afterBetweenTurns.getPendingActiveReplacement().isPresent()) {
            return afterBetweenTurns;
        }
        return new GameState(afterBetweenTurns.getGameId(), GameStatus.ACTIVE, afterBetweenTurns.getPlayerOneState(), afterBetweenTurns.getPlayerTwoState(), turn.preparedForNextPlayer(opponentOf(afterBetweenTurns, command.playerId())), afterBetweenTurns.getActiveStadium().orElse(null), afterBetweenTurns.getFinishResult().orElse(null), null, events);
    }

    private void requireActive(GameState state) {
        if (state.getStatus() != GameStatus.ACTIVE) {
            throw new TurnException("Game must be ACTIVE");
        }
    }

    private PlayerGameState getPlayerState(GameState state, PlayerId playerId) {
        if (state.getPlayerOneState().getPlayerId().equals(playerId)) {
            return state.getPlayerOneState();
        }
        if (state.getPlayerTwoState().getPlayerId().equals(playerId)) {
            return state.getPlayerTwoState();
        }
        throw new TurnException("Player is not part of this game");
    }

    private PlayerId opponentOf(GameState state, PlayerId playerId) {
        if (state.getPlayerOneState().getPlayerId().equals(playerId)) {
            return state.getPlayerTwoState().getPlayerId();
        }
        if (state.getPlayerTwoState().getPlayerId().equals(playerId)) {
            return state.getPlayerOneState().getPlayerId();
        }
        throw new TurnException("Player is not part of this game");
    }

    private GameState withUpdatedPlayer(GameState state, PlayerGameState updatedPlayer, GameStatus status, TurnState turnState, List<GameEvent> events) {
        return withUpdatedPlayer(state, updatedPlayer, status, turnState, null, events);
    }

    private GameState withUpdatedPlayer(GameState state, PlayerGameState updatedPlayer, GameStatus status, TurnState turnState, GameFinishResult finishResult, List<GameEvent> events) {
        PlayerGameState playerOneState = state.getPlayerOneState().getPlayerId().equals(updatedPlayer.getPlayerId()) ? updatedPlayer : state.getPlayerOneState();
        PlayerGameState playerTwoState = state.getPlayerTwoState().getPlayerId().equals(updatedPlayer.getPlayerId()) ? updatedPlayer : state.getPlayerTwoState();
        return new GameState(state.getGameId(), status, playerOneState, playerTwoState, turnState, state.getActiveStadium().orElse(null), finishResult, state.getPendingActiveReplacement().orElse(null), events);
    }
}
