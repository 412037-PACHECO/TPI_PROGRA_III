package com.tpi.pokemon.game.application;

public class InvalidGameCommandException extends RuntimeException {
    public InvalidGameCommandException(String message) {
        super(message);
    }
}
