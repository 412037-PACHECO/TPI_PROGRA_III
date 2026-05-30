package com.tpi.pokemon.game.engine.effect.mapping;

import java.util.Objects;
import java.util.Set;

public record Xy1AuditEntry(
        String cardId,
        String name,
        String supertype,
        String subtypes,
        String attacks,
        String abilities,
        String rules,
        String effectText,
        Set<Xy1EffectCategory> categories,
        Xy1EffectComplexity complexity,
        boolean supportedByCurrentEngine,
        String genericHandlers,
        boolean customHandlerRequired,
        Set<Xy1AuditStatus> statuses,
        boolean tested,
        String notes
) {
    public Xy1AuditEntry {
        if (cardId == null || cardId.isBlank()) {
            throw new IllegalArgumentException("cardId must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(complexity, "complexity must not be null");
        categories = Set.copyOf(Objects.requireNonNull(categories, "categories must not be null"));
        statuses = Set.copyOf(Objects.requireNonNull(statuses, "statuses must not be null"));
        subtypes = subtypes == null ? "" : subtypes;
        attacks = attacks == null ? "" : attacks;
        abilities = abilities == null ? "" : abilities;
        rules = rules == null ? "" : rules;
        effectText = effectText == null ? "" : effectText;
        genericHandlers = genericHandlers == null ? "" : genericHandlers;
        notes = notes == null ? "" : notes;
    }
}
