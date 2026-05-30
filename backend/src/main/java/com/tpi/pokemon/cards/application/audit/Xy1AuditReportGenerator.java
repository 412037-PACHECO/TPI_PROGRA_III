package com.tpi.pokemon.cards.application.audit;

import com.tpi.pokemon.cards.domain.CardEntity;
import com.tpi.pokemon.game.engine.effect.mapping.Xy1AuditStatus;
import com.tpi.pokemon.game.engine.effect.mapping.Xy1EffectCatalog;
import com.tpi.pokemon.game.engine.effect.mapping.Xy1EffectCategory;
import com.tpi.pokemon.game.engine.effect.mapping.Xy1EffectComplexity;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Xy1AuditReportGenerator {
    private final Xy1CardClassifier classifier;
    private final int expectedCardCount;

    public Xy1AuditReportGenerator(Xy1CardClassifier classifier) {
        this(classifier, Xy1EffectCatalog.EXPECTED_CARD_COUNT);
    }

    public Xy1AuditReportGenerator(Xy1CardClassifier classifier, int expectedCardCount) {
        this.classifier = Objects.requireNonNull(classifier, "classifier must not be null");
        this.expectedCardCount = expectedCardCount;
    }

    public Xy1AuditReport generate(List<CardEntity> cards) {
        Objects.requireNonNull(cards, "cards must not be null");
        List<Xy1CardAuditEntry> entries = cards.stream()
                .sorted((left, right) -> compareCardNumbers(left.getNumber(), right.getNumber()))
                .map(classifier::classify)
                .toList();
        List<Xy1UnsupportedEffectReport> unsupported = unsupportedEffects(entries);
        Map<Xy1EffectComplexity, Long> complexityCounts = new EnumMap<>(Xy1EffectComplexity.class);
        for (Xy1EffectComplexity complexity : Xy1EffectComplexity.values()) {
            complexityCounts.put(complexity, entries.stream().filter(entry -> entry.complexity() == complexity).count());
        }
        long mapped = entries.stream().filter(Xy1CardAuditEntry::hasExplicitMapping).count();
        long tested = entries.stream().filter(Xy1CardAuditEntry::tested).count();
        boolean allImported = entries.size() == expectedCardCount;
        boolean fullComplete = allImported && unsupported.isEmpty() && entries.stream().allMatch(entry -> entry.implementationStatus().contains(Xy1AuditStatus.FULLY_TESTED));
        return new Xy1AuditReport(Xy1EffectCatalog.SET_ID, expectedCardCount, entries.size(), allImported, fullComplete, entries, unsupported, complexityCounts, mapped, tested);
    }

    private List<Xy1UnsupportedEffectReport> unsupportedEffects(List<Xy1CardAuditEntry> entries) {
        List<Xy1UnsupportedEffectReport> reports = new ArrayList<>();
        for (Xy1CardAuditEntry card : entries) {
            card.attacks().forEach(attack -> addUnsupported(reports, card, "ATTACK", attack.name(), attack.effectCategories(), attack.supportedByCurrentEngine(), attack.customHandlerRequired()));
            card.abilities().forEach(ability -> addUnsupported(reports, card, "ABILITY", ability.name(), ability.effectCategories(), ability.supportedByCurrentEngine(), ability.customHandlerRequired()));
            card.rules().forEach(rule -> addUnsupported(reports, card, "RULE", "rule", rule.effectCategories(), rule.supportedByCurrentEngine(), rule.customHandlerRequired()));
        }
        return List.copyOf(reports);
    }

    private void addUnsupported(List<Xy1UnsupportedEffectReport> reports, Xy1CardAuditEntry card, String sourceType, String sourceName, java.util.Set<Xy1EffectCategory> categories, boolean supported, boolean customRequired) {
        if (supported && !customRequired) {
            return;
        }
        for (Xy1EffectCategory category : categories) {
            if (!supported || customRequired) {
                reports.add(new Xy1UnsupportedEffectReport(card.cardId(), card.name(), sourceType, sourceName, category, customRequired ? "Requires custom handler or infrastructure" : "No current generic handler/support"));
            }
        }
    }

    private int compareCardNumbers(String left, String right) {
        return Integer.compare(parseNumber(left), parseNumber(right));
    }

    private int parseNumber(String value) {
        if (value == null || value.isBlank()) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(value.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
