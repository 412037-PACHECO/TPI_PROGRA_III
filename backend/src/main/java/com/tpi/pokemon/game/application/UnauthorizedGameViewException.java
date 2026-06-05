package com.tpi.pokemon.game.application;

public class UnauthorizedGameViewException extends RuntimeException {
    public UnauthorizedGameViewException(String playerId, String gameId) {
        super("Player " + playerId + " is not allowed to view game " + gameId);
    }
}
