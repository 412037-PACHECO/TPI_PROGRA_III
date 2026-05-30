package com.tpi.pokemon.game.engine.damage;

import com.tpi.pokemon.game.domain.enums.PokemonType;
import com.tpi.pokemon.game.domain.model.AttackDefinition;
import com.tpi.pokemon.game.domain.model.CardDefinitionRef;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.model.Resistance;
import com.tpi.pokemon.game.domain.model.Weakness;
import com.tpi.pokemon.game.engine.effect.modifier.AppliedModifier;
import com.tpi.pokemon.game.engine.effect.modifier.DamageModifierContext;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierLayer;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierResolutionResult;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DamageCalculator {
    public DamageCalculation calculate(PokemonInPlay attacker, PokemonInPlay defender, AttackDefinition attack) {
        Objects.requireNonNull(attacker, "attacker must not be null");
        Objects.requireNonNull(defender, "defender must not be null");
        Objects.requireNonNull(attack, "attack must not be null");

        CardDefinitionRef attackerDefinition = attacker.getTopCard().definition();
        CardDefinitionRef defenderDefinition = defender.getTopCard().definition();
        List<PokemonType> attackerTypes = attackerDefinition.pokemonTypes();

        int damage = attack.baseDamage();
        boolean weaknessApplied = false;
        for (Weakness weakness : defenderDefinition.weaknesses()) {
            if (attackerTypes.contains(weakness.type())) {
                damage *= weakness.multiplier();
                weaknessApplied = true;
                break;
            }
        }

        boolean resistanceApplied = false;
        for (Resistance resistance : defenderDefinition.resistances()) {
            if (attackerTypes.contains(resistance.type())) {
                damage -= resistance.reduction();
                resistanceApplied = true;
                break;
            }
        }

        int finalDamage = Math.max(0, damage);
        return new DamageCalculation(attack.baseDamage(), weaknessApplied, resistanceApplied, finalDamage, finalDamage / 10);
    }

    public DamageCalculation calculate(DamageModifierContext context, ModifierResolver modifierResolver) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(modifierResolver, "modifierResolver must not be null");

        PokemonInPlay attacker = context.attacker();
        PokemonInPlay defender = context.defender();
        AttackDefinition attack = context.attack();
        CardDefinitionRef attackerDefinition = attacker.getTopCard().definition();
        CardDefinitionRef defenderDefinition = defender.getTopCard().definition();
        List<PokemonType> attackerTypes = attackerDefinition.pokemonTypes();
        List<AppliedModifier> modifiers = new ArrayList<>();

        int damage = attack.baseDamage();
        ModifierResolutionResult beforeWeakness = modifierResolver.resolveDamage(context, damage, ModifierLayer.BEFORE_WEAKNESS_RESISTANCE);
        damage = beforeWeakness.value();
        modifiers.addAll(beforeWeakness.appliedModifiers());

        boolean weaknessApplied = false;
        for (Weakness weakness : defenderDefinition.weaknesses()) {
            if (attackerTypes.contains(weakness.type())) {
                damage *= weakness.multiplier();
                weaknessApplied = true;
                break;
            }
        }

        boolean resistanceApplied = false;
        for (Resistance resistance : defenderDefinition.resistances()) {
            if (attackerTypes.contains(resistance.type())) {
                damage -= resistance.reduction();
                resistanceApplied = true;
                break;
            }
        }

        ModifierResolutionResult afterWeakness = modifierResolver.resolveDamage(context, Math.max(0, damage), ModifierLayer.AFTER_WEAKNESS_RESISTANCE);
        damage = afterWeakness.value();
        modifiers.addAll(afterWeakness.appliedModifiers());

        ModifierResolutionResult prevention = modifierResolver.resolveDamage(context, damage, ModifierLayer.PREVENTION);
        damage = prevention.value();
        modifiers.addAll(prevention.appliedModifiers());

        int finalDamage = Math.max(0, roundDownToTen(damage));
        return new DamageCalculation(attack.baseDamage(), weaknessApplied, resistanceApplied, finalDamage, finalDamage / 10, beforeWeakness.prevented() || afterWeakness.prevented() || prevention.prevented(), modifiers);
    }

    private int roundDownToTen(int value) {
        return value - Math.floorMod(value, 10);
    }
}
