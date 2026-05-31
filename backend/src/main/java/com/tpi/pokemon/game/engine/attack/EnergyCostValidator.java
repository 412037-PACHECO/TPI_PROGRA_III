package com.tpi.pokemon.game.engine.attack;

import com.tpi.pokemon.game.domain.enums.EnergyType;
import com.tpi.pokemon.game.domain.model.AttackDefinition;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.EnergyProfile;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EnergyCostValidator {
    public boolean hasEnoughEnergy(PokemonInPlay attacker, AttackDefinition attack) {
        Objects.requireNonNull(attacker, "attacker must not be null");
        Objects.requireNonNull(attack, "attack must not be null");

        AvailableEnergy available = availableEnergy(attacker);
        List<EnergyType> availableSymbols = new ArrayList<>(available.fixedSymbols());
        int flexibleSymbols = available.flexibleSymbolCount();
        for (EnergyType required : attack.cost()) {
            if (required == EnergyType.COLORLESS) {
                continue;
            }
            if (availableSymbols.remove(required)) {
                continue;
            }
            if (flexibleSymbols > 0) {
                flexibleSymbols--;
                continue;
            } else {
                return false;
            }
        }

        long colorlessRequired = attack.cost().stream().filter(EnergyType.COLORLESS::equals).count();
        return availableSymbols.size() + flexibleSymbols >= colorlessRequired;
    }

    private AvailableEnergy availableEnergy(PokemonInPlay attacker) {
        List<EnergyType> symbols = new ArrayList<>();
        int flexible = 0;
        for (CardInstance energy : attacker.getAttachedCards().getEnergies()) {
            if (energy.definition().isEnergy()) {
                EnergyProfile profile = energy.definition().energyProfile();
                if (profile.providesAnyTypeWhileAttached()) {
                    flexible++;
                } else {
                    symbols.addAll(profile.provides());
                }
            }
        }
        return new AvailableEnergy(symbols, flexible);
    }

    private record AvailableEnergy(List<EnergyType> fixedSymbols, int flexibleSymbolCount) {}
}
