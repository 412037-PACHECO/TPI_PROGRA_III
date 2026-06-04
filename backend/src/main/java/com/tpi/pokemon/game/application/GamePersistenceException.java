package com.tpi.pokemon.game.application;

public class GamePersistenceException extends RuntimeException {
    public GamePersistenceException(String message) {
        super(message);
    }

    public GamePersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
