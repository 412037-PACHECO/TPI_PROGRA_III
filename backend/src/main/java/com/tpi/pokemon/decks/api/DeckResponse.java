package com.tpi.pokemon.decks.api;

import java.time.Instant;

public record DeckResponse(Long id, String name, String ownerName, int totalCards, Instant createdAt, Instant updatedAt) {
}
