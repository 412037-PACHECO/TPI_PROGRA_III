package com.tpi.pokemon.cards.application.audit;

import com.tpi.pokemon.game.engine.effect.mapping.Xy1AuditStatus;
import com.tpi.pokemon.game.engine.effect.mapping.Xy1EffectCategory;
import com.tpi.pokemon.game.engine.effect.mapping.Xy1EffectComplexity;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record Xy1AttackAuditEntry(
        String name,
        String damage,
        String text,
        Set<Xy1EffectCategory> effectCategories,
        Xy1EffectComplexity complexity,
        boolean supportedByCurrentEngine,
        List<String> genericHandlersRequired,
        boolean customHandlerRequired,
        Set<Xy1AuditStatus> implementationStatus,
        boolean tested,
        List<String> notes
) {
    public Xy1AttackAuditEntry {
        name = name == null ? "" : name;
        damage = damage == null ? "" : damage;
        text = text == null ? "" : text;
        effectCategories = Set.copyOf(Objects.requireNonNull(effectCategories, "effectCategories must not be null"));
        Objects.requireNonNull(complexity, "complexity must not be null");
        genericHandlersRequired = List.copyOf(Objects.requireNonNull(genericHandlersRequired, "genericHandlersRequired must not be null"));
        implementationStatus = Set.copyOf(Objects.requireNonNull(implementationStatus, "implementationStatus must not be null"));
        notes = List.copyOf(Objects.requireNonNull(notes, "notes must not be null"));
    }
}
