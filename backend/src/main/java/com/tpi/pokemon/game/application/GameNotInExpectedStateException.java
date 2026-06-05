package com.tpi.pokemon.game.application;

public class GameNotInExpectedStateException extends RuntimeException {
    public GameNotInExpectedStateException(String message) {
        super(message);
    }
}
