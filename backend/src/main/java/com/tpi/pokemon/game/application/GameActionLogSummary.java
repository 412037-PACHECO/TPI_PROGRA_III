package com.tpi.pokemon.game.application;

import java.time.Instant;

public record GameActionLogSummary(
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
