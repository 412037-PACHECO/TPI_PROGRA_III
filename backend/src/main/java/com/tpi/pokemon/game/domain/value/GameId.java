package com.tpi.pokemon.game.domain.value;

public record GameId(String value) {
    public GameId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("GameId must not be blank");
        }
    }
}
