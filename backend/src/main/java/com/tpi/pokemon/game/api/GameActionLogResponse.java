package com.tpi.pokemon.game.api;

import java.time.Instant;

public record GameActionLogResponse(
        long sequence,
        int turnNumber,
        String phase,
        String actorPlayerId,
        String actionType,
        String commandJson,
        String resultJson,
        String eventsJson,
        Instant createdAt
) {
}
