package com.tpi.pokemon.game.engine.effect.modifier;

public interface ModifierResolver {
    ModifierResolutionResult resolveDamage(DamageModifierContext context, int currentDamage, ModifierLayer layer);

    ModifierResolutionResult resolveRetreatCost(RetreatCostModifierContext context);

    ModifierResolutionResult resolveSpecialConditionPrevention(SpecialConditionModifierContext context);
}
