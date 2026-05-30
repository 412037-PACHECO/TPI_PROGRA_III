package com.tpi.pokemon.game.engine.effect.modifier;

import java.util.Objects;

public record ModifierDefinition(
        ModifierType type,
        ModifierOperation operation,
        ModifierLayer layer,
        int amount,
        ModifierTargetRole targetRole
) {
    public ModifierDefinition {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(operation, "operation must not be null");
        layer = layer == null ? ModifierLayer.AFTER_WEAKNESS_RESISTANCE : layer;
        targetRole = targetRole == null ? ModifierTargetRole.DEFAULT_TARGET : targetRole;
    }
}
