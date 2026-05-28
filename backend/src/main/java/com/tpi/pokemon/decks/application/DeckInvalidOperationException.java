package com.tpi.pokemon.decks.application;

public class DeckInvalidOperationException extends RuntimeException {
    public DeckInvalidOperationException(String message) {
        super(message);
    }
}
