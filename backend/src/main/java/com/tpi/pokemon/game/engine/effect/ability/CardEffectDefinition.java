package com.tpi.pokemon.game.engine.effect.ability;

import com.tpi.pokemon.game.engine.effect.EffectTiming;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierDefinition;
import java.util.List;
import java.util.Objects;

public record CardEffectDefinition(
        String effectId,
        String name,
        EffectSourceKind sourceKind,
        EffectActivationKind activationKind,
        EffectTiming timing,
        EffectScope scope,
        EffectCondition condition,
        List<ModifierDefinition> modifiers
) {
    public CardEffectDefinition {
        if (effectId == null || effectId.isBlank()) {
            throw new IllegalArgumentException("effectId must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(sourceKind, "sourceKind must not be null");
        Objects.requireNonNull(activationKind, "activationKind must not be null");
        Objects.requireNonNull(timing, "timing must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        condition = condition == null ? EffectCondition.always() : condition;
        Objects.requireNonNull(modifiers, "modifiers must not be null");
        if (modifiers.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("modifiers must not contain null values");
        }
        modifiers = List.copyOf(modifiers);
    }
}
