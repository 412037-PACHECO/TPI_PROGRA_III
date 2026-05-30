package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record PendingEffectSelection(
        PlayerId playerId,
        EffectType effectType,
        String sourceId,
        EffectCardZone sourceZone,
        EffectTarget target,
        int minSelections,
        int maxSelections,
        CardFilterSpec cardFilter
) {
    public PendingEffectSelection {
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(effectType, "effectType must not be null");
        Objects.requireNonNull(target, "target must not be null");
        cardFilter = cardFilter == null ? CardFilterSpec.any() : cardFilter;
        if (minSelections < 0 || maxSelections < minSelections) {
            throw new IllegalArgumentException("selection bounds are invalid");
        }
    }
}
