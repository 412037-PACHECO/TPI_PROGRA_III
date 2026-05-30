package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.List;

public record CardDrawEffectResolvedEvent(GameId gameId, PlayerId playerId, int requestedCount, List<CardInstanceId> drawnCardIds) implements GameEvent {
    public CardDrawEffectResolvedEvent { drawnCardIds = List.copyOf(drawnCardIds); }
}
