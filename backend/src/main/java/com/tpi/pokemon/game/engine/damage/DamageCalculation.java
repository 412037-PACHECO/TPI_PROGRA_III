package com.tpi.pokemon.game.engine.damage;

public record DamageCalculation(
        int baseDamage,
        boolean weaknessApplied,
        boolean resistanceApplied,
        int finalDamage,
        int countersAdded
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
    }
}
