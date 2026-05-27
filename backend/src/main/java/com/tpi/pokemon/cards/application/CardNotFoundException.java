package com.tpi.pokemon.cards.application;

public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(String cardId) {
        super("No existe carta cacheada con cardId: " + cardId);
    }
}
