package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.enums.TurnPhase;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record TurnState(
        PlayerId currentPlayer,
        PlayerId startingPlayer,
        int turnNumber,
        TurnPhase phase,
        boolean cardDrawnThisTurn,
        boolean energyAttachedThisTurn,
        boolean supporterPlayedThisTurn,
        boolean stadiumPlayedThisTurn,
        boolean retreatedThisTurn
) {
    public TurnState {
        Objects.requireNonNull(phase, "phase must not be null");
        if (turnNumber < 0) {
            throw new IllegalArgumentException("turnNumber must not be negative");
        }
        if (startingPlayer == null && currentPlayer != null) {
            startingPlayer = currentPlayer;
        }
        if (phase == TurnPhase.NOT_STARTED) {
            if (cardDrawnThisTurn || energyAttachedThisTurn || supporterPlayedThisTurn || stadiumPlayedThisTurn || retreatedThisTurn) {
                throw new IllegalArgumentException("NOT_STARTED turn state must have false flags");
            }
        } else if (currentPlayer == null) {
            throw new IllegalArgumentException("currentPlayer is required after the turn starts");
        }
    }

    public TurnState(
            PlayerId currentPlayer,
            int turnNumber,
            TurnPhase phase,
            boolean energyAttachedThisTurn,
            boolean supporterPlayedThisTurn,
            boolean stadiumPlayedThisTurn,
            boolean retreatedThisTurn
    ) {
        this(currentPlayer, currentPlayer, turnNumber, phase, false, energyAttachedThisTurn, supporterPlayedThisTurn, stadiumPlayedThisTurn, retreatedThisTurn);
    }

    public static TurnState notStarted() {
        return new TurnState(null, null, 0, TurnPhase.NOT_STARTED, false, false, false, false, false);
    }

    public static TurnState preparedForFirstTurn(PlayerId startingPlayer) {
        Objects.requireNonNull(startingPlayer, "startingPlayer must not be null");
        return new TurnState(startingPlayer, startingPlayer, 0, TurnPhase.NOT_STARTED, false, false, false, false, false);
    }

    public TurnState startDrawPhase(int nextTurnNumber) {
        return new TurnState(currentPlayer, startingPlayer, nextTurnNumber, TurnPhase.DRAW, false, false, false, false, false);
    }

    public TurnState enterMain(boolean cardDrawn) {
        return new TurnState(currentPlayer, startingPlayer, turnNumber, TurnPhase.MAIN, cardDrawn, energyAttachedThisTurn, supporterPlayedThisTurn, stadiumPlayedThisTurn, retreatedThisTurn);
    }

    public TurnState withEnergyAttached() {
        return new TurnState(currentPlayer, startingPlayer, turnNumber, phase, cardDrawnThisTurn, true, supporterPlayedThisTurn, stadiumPlayedThisTurn, retreatedThisTurn);
    }

    public TurnState withSupporterPlayed() {
        return new TurnState(currentPlayer, startingPlayer, turnNumber, phase, cardDrawnThisTurn, energyAttachedThisTurn, true, stadiumPlayedThisTurn, retreatedThisTurn);
    }

    public TurnState withStadiumPlayed() {
        return new TurnState(currentPlayer, startingPlayer, turnNumber, phase, cardDrawnThisTurn, energyAttachedThisTurn, supporterPlayedThisTurn, true, retreatedThisTurn);
    }

    public TurnState withRetreated() {
        return new TurnState(currentPlayer, startingPlayer, turnNumber, phase, cardDrawnThisTurn, energyAttachedThisTurn, supporterPlayedThisTurn, stadiumPlayedThisTurn, true);
    }

    public TurnState preparedForNextPlayer(PlayerId nextPlayer) {
        Objects.requireNonNull(nextPlayer, "nextPlayer must not be null");
        return new TurnState(nextPlayer, startingPlayer, turnNumber, TurnPhase.NOT_STARTED, false, false, false, false, false);
    }
}
