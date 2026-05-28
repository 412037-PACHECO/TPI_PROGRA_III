package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.enums.TurnPhase;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record TurnState(
        PlayerId currentPlayer,
        int turnNumber,
        TurnPhase phase,
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
        if (phase == TurnPhase.NOT_STARTED) {
            if (turnNumber != 0 || energyAttachedThisTurn || supporterPlayedThisTurn || stadiumPlayedThisTurn || retreatedThisTurn) {
                throw new IllegalArgumentException("NOT_STARTED turn state must have turn number 0 and false flags");
            }
        } else if (currentPlayer == null) {
            throw new IllegalArgumentException("currentPlayer is required after the turn starts");
        }
    }

    public static TurnState notStarted() {
        return new TurnState(null, 0, TurnPhase.NOT_STARTED, false, false, false, false);
    }

    public static TurnState preparedForFirstTurn(PlayerId startingPlayer) {
        Objects.requireNonNull(startingPlayer, "startingPlayer must not be null");
        return new TurnState(startingPlayer, 0, TurnPhase.NOT_STARTED, false, false, false, false);
    }
}
