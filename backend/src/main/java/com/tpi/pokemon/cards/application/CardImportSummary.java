package com.tpi.pokemon.cards.application;

public record CardImportSummary(int received, int created, int updated, int skipped, int errors) {
}
