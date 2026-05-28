package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.enums.TurnPhase;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record TurnPhaseChangedEvent(GameId gameId, PlayerId currentPlayer, TurnPhase previousPhase, TurnPhase newPhase) implements GameEvent {
    public TurnPhaseChangedEvent {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(currentPlayer, "currentPlayer must not be null");
        Objects.requireNonNull(previousPhase, "previousPhase must not be null");
        Objects.requireNonNull(newPhase, "newPhase must not be null");
    }
}
