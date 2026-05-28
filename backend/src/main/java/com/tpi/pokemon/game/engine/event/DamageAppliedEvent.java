package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import java.util.Objects;

public record DamageAppliedEvent(GameId gameId, CardInstanceId targetId, int damageAmount, int countersAdded, int totalDamageCounters) implements GameEvent {
    public DamageAppliedEvent {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(targetId, "targetId must not be null");
        if (damageAmount < 0 || damageAmount % 10 != 0) {
            throw new IllegalArgumentException("damageAmount must be non-negative and a multiple of 10");
        }
        if (countersAdded < 0 || totalDamageCounters < 0) {
            throw new IllegalArgumentException("damage counters must not be negative");
        }
    }
}
