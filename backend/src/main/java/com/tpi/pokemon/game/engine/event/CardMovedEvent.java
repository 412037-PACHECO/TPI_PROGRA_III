package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.enums.ZoneType;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record CardMovedEvent(GameId gameId, PlayerId playerId, CardInstanceId cardId, ZoneType fromZone, ZoneType toZone) implements GameEvent {
    public CardMovedEvent {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(cardId, "cardId must not be null");
        Objects.requireNonNull(fromZone, "fromZone must not be null");
        Objects.requireNonNull(toZone, "toZone must not be null");
    }
}
