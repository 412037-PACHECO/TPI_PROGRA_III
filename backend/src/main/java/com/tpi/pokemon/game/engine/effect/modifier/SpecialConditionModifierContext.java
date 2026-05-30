package com.tpi.pokemon.game.engine.effect.modifier;

import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record SpecialConditionModifierContext(GameState state, PlayerId sourcePlayerId, PlayerId targetPlayerId, PokemonInPlay target, SpecialCondition condition) {
    public SpecialConditionModifierContext {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(sourcePlayerId, "sourcePlayerId must not be null");
        Objects.requireNonNull(targetPlayerId, "targetPlayerId must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(condition, "condition must not be null");
    }
}
