package com.tpi.pokemon.game.application;

import java.time.Instant;

public record GameSessionSummary(
        String gameId,
        String playerOneId,
        String playerTwoId,
        String status,
        String currentPlayerId,
        int turnNumber,
        String phase,
        String winnerId,
        Instant createdAt,
        Instant updatedAt
) {
}
