package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record AttackDeclaredEvent(GameId gameId, PlayerId playerId, CardInstanceId attackerId, String attackId, String attackName) implements GameEvent {
    public AttackDeclaredEvent {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(attackerId, "attackerId must not be null");
        if (attackId == null || attackId.isBlank()) {
            throw new IllegalArgumentException("attackId must not be blank");
        }
        if (attackName == null || attackName.isBlank()) {
            throw new IllegalArgumentException("attackName must not be blank");
        }
    }
}
