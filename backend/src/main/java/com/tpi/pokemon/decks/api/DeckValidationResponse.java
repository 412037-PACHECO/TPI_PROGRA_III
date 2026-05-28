package com.tpi.pokemon.decks.api;

import java.util.List;

public record DeckValidationResponse(boolean valid, int totalCards, List<DeckValidationError> errors, List<String> warnings) {
}
