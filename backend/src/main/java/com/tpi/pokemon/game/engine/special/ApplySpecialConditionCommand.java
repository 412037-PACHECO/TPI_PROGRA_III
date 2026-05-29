package com.tpi.pokemon.game.engine.special;

import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.action.PokemonTarget;
import java.util.Objects;

public record ApplySpecialConditionCommand(PlayerId playerId, PokemonTarget target, SpecialCondition condition) {
    public ApplySpecialConditionCommand {
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(condition, "condition must not be null");
    }
}
