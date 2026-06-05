package com.tpi.pokemon.game.realtime;

import com.tpi.pokemon.game.application.view.GameViewResponse;
import java.time.Instant;
import java.util.UUID;

public record GameViewUpdatedEvent(String eventId, String gameId, String playerId, Instant createdAt, GameViewResponse view) {
    public static GameViewUpdatedEvent from(GameViewResponse view) {
        return new GameViewUpdatedEvent(UUID.randomUUID().toString(), view.gameId(), view.viewerPlayerId(), Instant.now(), view);
    }
}
