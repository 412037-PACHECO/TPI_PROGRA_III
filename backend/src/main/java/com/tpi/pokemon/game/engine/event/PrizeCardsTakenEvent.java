package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.List;

public record PrizeCardsTakenEvent(GameId gameId, PlayerId playerId, List<CardInstanceId> prizeCardIds, int remainingPrizeCount) implements GameEvent {
    public PrizeCardsTakenEvent {
        prizeCardIds = List.copyOf(prizeCardIds);
    }
}
