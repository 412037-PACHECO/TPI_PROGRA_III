package com.tpi.pokemon.game.engine.command;

import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.engine.event.GameEvent;
import java.util.List;
import java.util.Objects;

public record CommandResult(boolean success, GameState gameState, List<GameEvent> events, String error) {
    public CommandResult {
        Objects.requireNonNull(events, "events must not be null");
        if (events.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("events must not contain null values");
        }
        if (success && gameState == null) {
            throw new IllegalArgumentException("Successful command result must include gameState");
        }
        if (!success && (error == null || error.isBlank())) {
            throw new IllegalArgumentException("Failed command result must include error");
        }
        events = List.copyOf(events);
    }

    public static CommandResult success(GameState gameState, List<GameEvent> events) {
        return new CommandResult(true, gameState, events, null);
    }

    public static CommandResult failure(String error) {
        return new CommandResult(false, null, List.of(), error);
    }
}
