package com.tpi.pokemon.game.engine.effect.modifier;

import java.util.List;
import java.util.Objects;

public record ModifierResolutionResult(int value, boolean prevented, List<AppliedModifier> appliedModifiers) {
    public ModifierResolutionResult {
        if (value < 0) {
            value = 0;
        }
        Objects.requireNonNull(appliedModifiers, "appliedModifiers must not be null");
        if (appliedModifiers.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("appliedModifiers must not contain null values");
        }
        appliedModifiers = List.copyOf(appliedModifiers);
    }

    public static ModifierResolutionResult unchanged(int value) {
        return new ModifierResolutionResult(value, false, List.of());
    }
}
