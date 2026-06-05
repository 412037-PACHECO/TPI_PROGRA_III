package com.tpi.pokemon.game.realtime;

import com.tpi.pokemon.game.application.view.GameLogPublicView;
import java.time.Instant;
import java.util.List;

public record GameLogUpdatedEvent(String eventId, String gameId, String playerId, Instant createdAt, List<GameLogPublicView> log) {
}
