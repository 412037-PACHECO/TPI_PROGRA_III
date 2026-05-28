package com.tpi.pokemon.decks.application;

public class DeckCardNotFoundException extends RuntimeException {
    public DeckCardNotFoundException(Long deckId, String cardId) {
        super("La carta " + cardId + " no está en el mazo " + deckId);
    }
}
