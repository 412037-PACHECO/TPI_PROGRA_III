package com.tpi.pokemon.game.engine.effect.ability;

import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import java.util.Objects;

public record EffectCondition(EffectConditionType type, SpecialCondition condition) {
    public EffectCondition {
        Objects.requireNonNull(type, "type must not be null");
        if (type != EffectConditionType.ALWAYS) {
            Objects.requireNonNull(condition, "condition must not be null for conditional effects");
        }
    }

    public static EffectCondition always() {
        return new EffectCondition(EffectConditionType.ALWAYS, null);
    }

    public boolean matches(PokemonInPlay target) {
        Objects.requireNonNull(target, "target must not be null");
        return switch (type) {
            case ALWAYS -> true;
            case TARGET_HAS_SPECIAL_CONDITION -> target.hasSpecialCondition(condition);
            case TARGET_DOES_NOT_HAVE_SPECIAL_CONDITION -> !target.hasSpecialCondition(condition);
        };
    }
}
