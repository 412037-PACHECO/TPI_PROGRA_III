package com.tpi.pokemon.game.domain.value;

public record CardInstanceId(String value) {
    public CardInstanceId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CardInstanceId must not be blank");
        }
    }
}
