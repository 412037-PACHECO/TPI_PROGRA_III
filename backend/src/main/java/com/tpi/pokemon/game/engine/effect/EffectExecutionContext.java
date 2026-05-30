package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.random.CoinFlipProvider;
import java.util.List;
import java.util.Objects;

public record EffectExecutionContext(GameState state, PlayerId actingPlayerId, PlayerId defendingPlayerId, String sourceId, List<GameEvent> events, CoinFlipProvider coinFlipProvider) {
    public EffectExecutionContext {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(actingPlayerId, "actingPlayerId must not be null");
        Objects.requireNonNull(defendingPlayerId, "defendingPlayerId must not be null");
        Objects.requireNonNull(events, "events must not be null");
        Objects.requireNonNull(coinFlipProvider, "coinFlipProvider must not be null");
    }

    public EffectExecutionContext withState(GameState state) {
        return new EffectExecutionContext(state, actingPlayerId, defendingPlayerId, sourceId, events, coinFlipProvider);
    }
}
