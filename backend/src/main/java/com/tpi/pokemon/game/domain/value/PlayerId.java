package com.tpi.pokemon.game.domain.value;

public record PlayerId(String value) {
    public PlayerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PlayerId must not be blank");
        }
    }
}
