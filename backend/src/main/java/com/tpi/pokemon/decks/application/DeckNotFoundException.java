package com.tpi.pokemon.decks.application;

public class DeckNotFoundException extends RuntimeException {
    public DeckNotFoundException(Long deckId) {
        super("No existe el mazo con id " + deckId);
    }
}
