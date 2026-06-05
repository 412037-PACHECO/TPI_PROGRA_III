package com.tpi.pokemon.game.api;

import java.time.Instant;

public record GameResponse(
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
