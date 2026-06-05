package com.tpi.pokemon.game.application;

public class UnauthorizedGameActionException extends RuntimeException {
    public UnauthorizedGameActionException(String message) {
        super(message);
    }
}
