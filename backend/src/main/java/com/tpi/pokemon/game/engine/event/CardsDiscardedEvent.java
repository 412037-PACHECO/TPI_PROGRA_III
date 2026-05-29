package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.List;

public record CardsDiscardedEvent(GameId gameId, PlayerId playerId, List<CardInstanceId> cardIds, String reason) implements GameEvent {
    public CardsDiscardedEvent {
        cardIds = List.copyOf(cardIds);
    }
}
