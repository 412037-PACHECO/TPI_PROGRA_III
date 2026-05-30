package com.tpi.pokemon.game.engine.effect.ability;

import com.tpi.pokemon.game.domain.enums.EnergyType;
import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import java.util.Objects;

public record EffectCondition(EffectConditionType type, SpecialCondition condition, EnergyType energyType) {
    public EffectCondition {
        Objects.requireNonNull(type, "type must not be null");
        if ((type == EffectConditionType.TARGET_HAS_SPECIAL_CONDITION || type == EffectConditionType.TARGET_DOES_NOT_HAVE_SPECIAL_CONDITION)) {
            Objects.requireNonNull(condition, "condition must not be null for conditional effects");
        }
        if (type == EffectConditionType.TARGET_HAS_ATTACHED_ENERGY_PROVIDING) {
            Objects.requireNonNull(energyType, "energyType must not be null for attached energy conditions");
        }
    }

    public EffectCondition(EffectConditionType type, SpecialCondition condition) {
        this(type, condition, null);
    }

    public static EffectCondition always() {
        return new EffectCondition(EffectConditionType.ALWAYS, null, null);
    }

    public static EffectCondition targetHasAttachedEnergyProviding(EnergyType energyType) {
        return new EffectCondition(EffectConditionType.TARGET_HAS_ATTACHED_ENERGY_PROVIDING, null, energyType);
    }

    public boolean matches(PokemonInPlay target) {
        Objects.requireNonNull(target, "target must not be null");
        return switch (type) {
            case ALWAYS -> true;
            case TARGET_HAS_SPECIAL_CONDITION -> target.hasSpecialCondition(condition);
            case TARGET_DOES_NOT_HAVE_SPECIAL_CONDITION -> !target.hasSpecialCondition(condition);
            case TARGET_HAS_ATTACHED_ENERGY_PROVIDING -> target.getAttachedCards().getEnergies().stream()
                    .anyMatch(card -> card.definition().energyProfile().provides().contains(energyType));
        };
    }
}
