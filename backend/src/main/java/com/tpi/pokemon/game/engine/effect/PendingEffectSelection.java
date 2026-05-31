package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import java.util.List;
import java.util.Objects;

public record PendingEffectSelection(
        PlayerId playerId,
        EffectType effectType,
        String sourceId,
        EffectCardZone sourceZone,
        EffectTarget target,
        int minSelections,
        int maxSelections,
        CardFilterSpec cardFilter,
        boolean revealSelectedCards,
        boolean requiresShuffle,
        EffectDefinition continuationEffect,
        List<CardInstanceId> candidateCardIds
) {
    public PendingEffectSelection {
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(effectType, "effectType must not be null");
        Objects.requireNonNull(target, "target must not be null");
        cardFilter = cardFilter == null ? CardFilterSpec.any() : cardFilter;
        if (minSelections < 0 || maxSelections < minSelections) {
            throw new IllegalArgumentException("selection bounds are invalid");
        }
        candidateCardIds = candidateCardIds == null ? List.of() : List.copyOf(candidateCardIds);
    }

    public PendingEffectSelection(PlayerId playerId, EffectType effectType, String sourceId, EffectCardZone sourceZone, EffectTarget target, int minSelections, int maxSelections, CardFilterSpec cardFilter) {
        this(playerId, effectType, sourceId, sourceZone, target, minSelections, maxSelections, cardFilter, false, false, null, List.of());
    }

    public PendingEffectSelection(PlayerId playerId, EffectType effectType, String sourceId, EffectCardZone sourceZone, EffectTarget target, int minSelections, int maxSelections, CardFilterSpec cardFilter, boolean revealSelectedCards, boolean requiresShuffle, EffectDefinition continuationEffect) {
        this(playerId, effectType, sourceId, sourceZone, target, minSelections, maxSelections, cardFilter, revealSelectedCards, requiresShuffle, continuationEffect, List.of());
    }
}
