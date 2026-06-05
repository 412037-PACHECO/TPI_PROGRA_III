package com.tpi.pokemon.game.application;

public class GameAlreadyFullException extends RuntimeException {
    public GameAlreadyFullException(String gameId) {
        super("Game " + gameId + " is already full");
    }
}
