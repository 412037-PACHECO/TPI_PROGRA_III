package com.tpi.pokemon.game.engine.effect.modifier;

import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record RetreatCostModifierContext(GameState state, PlayerId playerId, PokemonInPlay pokemon, int printedCost) {
    public RetreatCostModifierContext {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        if (printedCost < 0) {
            throw new IllegalArgumentException("printedCost must not be negative");
        }
    }
}
