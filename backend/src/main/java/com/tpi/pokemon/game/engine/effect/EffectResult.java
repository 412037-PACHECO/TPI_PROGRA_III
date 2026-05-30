package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.GameState;
import java.util.Objects;

public record EffectResult(GameState state) {
    public EffectResult {
        Objects.requireNonNull(state, "state must not be null");
    }
}
