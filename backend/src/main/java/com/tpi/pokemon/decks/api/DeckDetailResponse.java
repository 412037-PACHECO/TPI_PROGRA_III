package com.tpi.pokemon.decks.api;

import java.time.Instant;
import java.util.List;

public record DeckDetailResponse(Long id, String name, String ownerName, int totalCards, Instant createdAt, Instant updatedAt, List<DeckCardResponse> cards) {
}
