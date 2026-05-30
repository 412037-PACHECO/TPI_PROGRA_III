package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.List;

public record DeckSearchedEvent(GameId gameId, PlayerId playerId, List<CardInstanceId> selectedCardIds, int requestedCount, boolean revealSelectedCards, boolean requiresShuffle) implements GameEvent {
    public DeckSearchedEvent {
        selectedCardIds = List.copyOf(selectedCardIds);
    }
}
