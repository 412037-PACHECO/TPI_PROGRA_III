package com.tpi.pokemon.game.application;

public class DeckNotValidForGameException extends RuntimeException {
    public DeckNotValidForGameException(String message) {
        super(message);
    }
}
