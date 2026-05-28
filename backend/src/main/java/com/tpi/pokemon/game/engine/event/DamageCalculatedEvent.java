package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import java.util.Objects;

public record DamageCalculatedEvent(GameId gameId, CardInstanceId attackerId, CardInstanceId defenderId, int baseDamage, boolean weaknessApplied, boolean resistanceApplied, int finalDamage) implements GameEvent {
    public DamageCalculatedEvent {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(attackerId, "attackerId must not be null");
        Objects.requireNonNull(defenderId, "defenderId must not be null");
        if (baseDamage < 0 || baseDamage % 10 != 0) {
            throw new IllegalArgumentException("baseDamage must be non-negative and a multiple of 10");
        }
        if (finalDamage < 0 || finalDamage % 10 != 0) {
            throw new IllegalArgumentException("finalDamage must be non-negative and a multiple of 10");
        }
    }
}
