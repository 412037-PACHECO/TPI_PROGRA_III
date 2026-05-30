package com.tpi.pokemon.game.engine.effect.mapping;

import com.tpi.pokemon.game.engine.effect.EffectDefinition;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record AttackEffectMapping(
        String cardId,
        String cardName,
        String attackId,
        String attackName,
        String effectText,
        Set<Xy1EffectCategory> categories,
        Xy1EffectComplexity complexity,
        List<EffectDefinition> effects,
        Set<Xy1AuditStatus> statuses,
        boolean tested,
        String notes
) {
    public AttackEffectMapping {
        if (cardId == null || cardId.isBlank()) {
            throw new IllegalArgumentException("cardId must not be blank");
        }
        if (cardName == null || cardName.isBlank()) {
            throw new IllegalArgumentException("cardName must not be blank");
        }
        if (attackId == null || attackId.isBlank()) {
            throw new IllegalArgumentException("attackId must not be blank");
        }
        if (attackName == null || attackName.isBlank()) {
            throw new IllegalArgumentException("attackName must not be blank");
        }
        Objects.requireNonNull(complexity, "complexity must not be null");
        categories = Set.copyOf(Objects.requireNonNull(categories, "categories must not be null"));
        effects = List.copyOf(Objects.requireNonNull(effects, "effects must not be null"));
        statuses = Set.copyOf(Objects.requireNonNull(statuses, "statuses must not be null"));
        notes = notes == null ? "" : notes;
    }
}
