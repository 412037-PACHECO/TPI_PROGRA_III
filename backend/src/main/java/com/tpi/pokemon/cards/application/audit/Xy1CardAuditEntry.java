package com.tpi.pokemon.cards.application.audit;

import com.tpi.pokemon.game.engine.effect.mapping.Xy1AuditStatus;
import com.tpi.pokemon.game.engine.effect.mapping.Xy1EffectCategory;
import com.tpi.pokemon.game.engine.effect.mapping.Xy1EffectComplexity;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record Xy1CardAuditEntry(
        String cardId,
        String name,
        String supertype,
        String subtypes,
        String number,
        List<Xy1AttackAuditEntry> attacks,
        List<Xy1AbilityAuditEntry> abilities,
        List<Xy1RuleAuditEntry> rules,
        Set<Xy1EffectCategory> effectCategories,
        Xy1EffectComplexity complexity,
        boolean supportedByCurrentEngine,
        boolean hasExplicitMapping,
        boolean customHandlerRequired,
        Set<Xy1AuditStatus> implementationStatus,
        boolean tested,
        List<String> notes
) {
    public Xy1CardAuditEntry {
        cardId = cardId == null ? "" : cardId;
        name = name == null ? "" : name;
        supertype = supertype == null ? "" : supertype;
        subtypes = subtypes == null ? "" : subtypes;
        number = number == null ? "" : number;
        attacks = List.copyOf(Objects.requireNonNull(attacks, "attacks must not be null"));
        abilities = List.copyOf(Objects.requireNonNull(abilities, "abilities must not be null"));
        rules = List.copyOf(Objects.requireNonNull(rules, "rules must not be null"));
        effectCategories = Set.copyOf(Objects.requireNonNull(effectCategories, "effectCategories must not be null"));
        Objects.requireNonNull(complexity, "complexity must not be null");
        implementationStatus = Set.copyOf(Objects.requireNonNull(implementationStatus, "implementationStatus must not be null"));
        notes = List.copyOf(Objects.requireNonNull(notes, "notes must not be null"));
    }
}
