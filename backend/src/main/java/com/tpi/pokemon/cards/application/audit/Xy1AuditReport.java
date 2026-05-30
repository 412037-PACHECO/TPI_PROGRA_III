package com.tpi.pokemon.cards.application.audit;

import com.tpi.pokemon.game.engine.effect.mapping.Xy1EffectComplexity;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Xy1AuditReport(
        String setId,
        int expectedCardCount,
        int importedCardCount,
        boolean allExpectedCardsImported,
        boolean fullSetImplementationComplete,
        List<Xy1CardAuditEntry> cards,
        List<Xy1UnsupportedEffectReport> unsupportedEffects,
        Map<Xy1EffectComplexity, Long> complexityCounts,
        long mappedCardCount,
        long fullyTestedCardCount
) {
    public Xy1AuditReport {
        setId = setId == null ? "" : setId;
        cards = List.copyOf(Objects.requireNonNull(cards, "cards must not be null"));
        unsupportedEffects = List.copyOf(Objects.requireNonNull(unsupportedEffects, "unsupportedEffects must not be null"));
        complexityCounts = Map.copyOf(Objects.requireNonNull(complexityCounts, "complexityCounts must not be null"));
    }
}
