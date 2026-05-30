package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import java.util.Objects;

public record DamageModifiedEvent(GameId gameId, CardInstanceId sourceCardId, String effectId, int valueBefore, int valueAfter) implements GameEvent {
    public DamageModifiedEvent {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(sourceCardId, "sourceCardId must not be null");
        if (effectId == null || effectId.isBlank()) {
            throw new IllegalArgumentException("effectId must not be blank");
        }
    }
}
