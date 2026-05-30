package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.enums.EnergyType;
import com.tpi.pokemon.game.engine.effect.EffectDefinition;
import java.util.List;
import java.util.Objects;

public record AttackDefinition(String attackId, String name, List<EnergyType> cost, int baseDamage, List<EffectDefinition> effects) {
    public AttackDefinition {
        if (attackId == null || attackId.isBlank()) {
            throw new IllegalArgumentException("attackId must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("attack name must not be blank");
        }
        Objects.requireNonNull(cost, "cost must not be null");
        if (cost.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("cost must not contain null values");
        }
        if (baseDamage < 0 || baseDamage % 10 != 0) {
            throw new IllegalArgumentException("baseDamage must be non-negative and a multiple of 10");
        }
        Objects.requireNonNull(effects, "effects must not be null");
        if (effects.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("effects must not contain null values");
        }
        cost = List.copyOf(cost);
        effects = List.copyOf(effects);
    }

    public AttackDefinition(String attackId, String name, List<EnergyType> cost, int baseDamage) {
        this(attackId, name, cost, baseDamage, List.of());
    }
}
