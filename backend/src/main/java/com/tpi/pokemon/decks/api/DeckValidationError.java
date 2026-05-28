package com.tpi.pokemon.decks.api;

public record DeckValidationError(String code, String cardId, String cardName, String message) {
}
