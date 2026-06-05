package com.tpi.pokemon.game.realtime;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record GameRealtimeEvent(
        String eventId,
        String gameId,
        GameRealtimeEventType type,
        String playerId,
        Instant createdAt,
        String summary,
        Map<String, Object> payload
) {
    public static GameRealtimeEvent of(String gameId, GameRealtimeEventType type, String playerId, String summary, Map<String, Object> payload) {
        return new GameRealtimeEvent(UUID.randomUUID().toString(), gameId, type, playerId, Instant.now(), summary, payload == null ? Map.of() : Map.copyOf(payload));
    }
}
