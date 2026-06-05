package com.tpi.pokemon.game.application;

public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(String gameId) {
        super("Game " + gameId + " was not found");
    }
}
