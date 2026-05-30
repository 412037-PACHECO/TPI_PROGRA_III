package com.tpi.pokemon.game.engine.special;

import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierResolutionResult;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierResolver;
import com.tpi.pokemon.game.engine.effect.modifier.SpecialConditionModifierContext;
import java.util.Objects;

public final class StatusEffectManager {
    public PokemonInPlay applyCondition(PokemonInPlay pokemon, SpecialCondition condition) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        Objects.requireNonNull(condition, "condition must not be null");
        return pokemon.applySpecialCondition(condition);
    }

    public SpecialConditionApplication applyCondition(SpecialConditionModifierContext context, ModifierResolver modifierResolver) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(modifierResolver, "modifierResolver must not be null");
        ModifierResolutionResult prevention = modifierResolver.resolveSpecialConditionPrevention(context);
        if (prevention.prevented()) {
            return new SpecialConditionApplication(context.target(), true, prevention.appliedModifiers());
        }
        return new SpecialConditionApplication(applyCondition(context.target(), context.condition()), false, prevention.appliedModifiers());
    }

    public PokemonInPlay removeCondition(PokemonInPlay pokemon, SpecialCondition condition) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        Objects.requireNonNull(condition, "condition must not be null");
        return pokemon.removeSpecialCondition(condition);
    }

    public PokemonInPlay clearSpecialConditions(PokemonInPlay pokemon) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        return pokemon.clearSpecialConditions();
    }

    public boolean hasCondition(PokemonInPlay pokemon, SpecialCondition condition) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        Objects.requireNonNull(condition, "condition must not be null");
        return pokemon.hasSpecialCondition(condition);
    }

    public boolean canAttack(PokemonInPlay pokemon) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        return !pokemon.hasSpecialCondition(SpecialCondition.ASLEEP) && !pokemon.hasSpecialCondition(SpecialCondition.PARALYZED);
    }

    public boolean canRetreat(PokemonInPlay pokemon) {
        return canAttack(pokemon);
    }
}
