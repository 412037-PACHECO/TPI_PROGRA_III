package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.engine.victory.FinishReason;
import java.util.List;

public record SuddenDeathRequiredEvent(GameId gameId, List<FinishReason> reasons) implements GameEvent {
    public SuddenDeathRequiredEvent {
        reasons = List.copyOf(reasons);
    }
}
