package com.tpi.pokemon.game.engine.damage;

import com.tpi.pokemon.game.engine.effect.modifier.AppliedModifier;
import java.util.List;
import java.util.Objects;

public record DamageCalculation(
        int baseDamage,
        boolean weaknessApplied,
        boolean resistanceApplied,
        int finalDamage,
        int countersAdded,
        boolean prevented,
        List<AppliedModifier> appliedModifiers
) {
    public DamageCalculation {
        if (baseDamage < 0 || baseDamage % 10 != 0) {
            throw new IllegalArgumentException("baseDamage must be non-negative and a multiple of 10");
        }
        if (finalDamage < 0 || finalDamage % 10 != 0) {
            throw new IllegalArgumentException("finalDamage must be non-negative and a multiple of 10");
        }
        if (countersAdded < 0) {
            throw new IllegalArgumentException("countersAdded must not be negative");
        }
        Objects.requireNonNull(appliedModifiers, "appliedModifiers must not be null");
        if (appliedModifiers.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("appliedModifiers must not contain null values");
        }
        appliedModifiers = List.copyOf(appliedModifiers);
    }

    public DamageCalculation(int baseDamage, boolean weaknessApplied, boolean resistanceApplied, int finalDamage, int countersAdded) {
        this(baseDamage, weaknessApplied, resistanceApplied, finalDamage, countersAdded, false, List.of());
    }
}
