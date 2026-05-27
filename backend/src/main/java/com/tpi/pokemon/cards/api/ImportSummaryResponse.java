package com.tpi.pokemon.cards.api;

public record ImportSummaryResponse(int received, int created, int updated, int skipped, int errors) {
}
