package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.GameState;
import java.util.Objects;
import java.util.Optional;

public record EffectResult(GameState state, PendingEffectSelection pendingSelection) {
    public EffectResult {
        Objects.requireNonNull(state, "state must not be null");
    }

    public EffectResult(GameState state) {
        this(state, null);
    }

    public Optional<PendingEffectSelection> pendingSelectionOptional() {
        return Optional.ofNullable(pendingSelection);
    }
}
