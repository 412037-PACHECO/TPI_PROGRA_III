package com.tpi.pokemon.decks.api;

public record DeckCardResponse(String cardId, String name, String supertype, String subtypes, String imageSmall, int quantity) {
}
