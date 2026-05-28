package com.tpi.pokemon.game.engine.damage;

import com.tpi.pokemon.game.domain.enums.PokemonType;
import com.tpi.pokemon.game.domain.model.AttackDefinition;
import com.tpi.pokemon.game.domain.model.CardDefinitionRef;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.model.Resistance;
import com.tpi.pokemon.game.domain.model.Weakness;
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
}
