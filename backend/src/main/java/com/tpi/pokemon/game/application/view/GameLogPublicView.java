package com.tpi.pokemon.game.application.view;

import java.time.Instant;
import java.util.List;

public record GameLogPublicView(
        long sequence,
        int turnNumber,
        String playerId,
        String actionType,
        Instant createdAt,
        String summary,
        List<String> publicEvents
) {
}
