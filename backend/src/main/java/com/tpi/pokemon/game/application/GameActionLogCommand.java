package com.tpi.pokemon.game.application;

import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;

public record GameActionLogCommand(
        GameId gameId,
        PlayerId actorPlayerId,
        String actionType,
        Object commandPayload,
        Object resultPayload,
        Object eventsPayload
) {
    public GameActionLogCommand {
        java.util.Objects.requireNonNull(gameId, "gameId must not be null");
        java.util.Objects.requireNonNull(commandPayload, "commandPayload must not be null");
        java.util.Objects.requireNonNull(resultPayload, "resultPayload must not be null");
        if (actionType == null || actionType.isBlank()) {
            throw new IllegalArgumentException("actionType must not be blank");
        }
    }
}
