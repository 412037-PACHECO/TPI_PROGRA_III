package com.tpi.pokemon.game.engine.attack;

import com.tpi.pokemon.game.domain.enums.EnergyType;
import com.tpi.pokemon.game.domain.model.AttackDefinition;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EnergyCostValidator {
    public boolean hasEnoughEnergy(PokemonInPlay attacker, AttackDefinition attack) {
        Objects.requireNonNull(attacker, "attacker must not be null");
        Objects.requireNonNull(attack, "attack must not be null");

        List<EnergyType> availableSymbols = availableSymbols(attacker);
        for (EnergyType required : attack.cost()) {
            if (required == EnergyType.COLORLESS) {
                continue;
            }
            if (!availableSymbols.remove(required)) {
                return false;
            }
        }

        long colorlessRequired = attack.cost().stream().filter(EnergyType.COLORLESS::equals).count();
        return availableSymbols.size() >= colorlessRequired;
    }

    private List<EnergyType> availableSymbols(PokemonInPlay attacker) {
        List<EnergyType> symbols = new ArrayList<>();
        for (CardInstance energy : attacker.getAttachedCards().getEnergies()) {
            if (energy.definition().isEnergy()) {
                symbols.addAll(energy.definition().energyProfile().provides());
            }
        }
        return symbols;
    }
}
