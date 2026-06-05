package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.List;

public record ResolvePendingEffectSelectionCommand(
        PlayerId playerId,
        List<CardInstanceId> selectedCardIds,
        Integer targetBenchIndex
) {
    public ResolvePendingEffectSelectionCommand {
        selectedCardIds = selectedCardIds == null ? List.of() : List.copyOf(selectedCardIds);
    }
}
