package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import java.util.List;
import java.util.Objects;

public record EffectDefinition(
        EffectType type,
        EffectTiming timing,
        EffectTarget target,
        int amount,
        SpecialCondition condition,
        List<EffectDefinition> children,
        EffectDefinition headsEffect,
        EffectDefinition tailsEffect,
        List<CardInstanceId> selectedCardIds,
        EffectCardZone sourceZone,
        EffectCardZone destinationZone,
        CardFilterSpec cardFilter,
        int targetBenchIndex,
        int sourceBenchIndex,
        int destinationBenchIndex,
        boolean revealSelectedCards,
        boolean requiresShuffle
) {
    public EffectDefinition {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(timing, "timing must not be null");
        children = children == null ? List.of() : List.copyOf(children);
        selectedCardIds = selectedCardIds == null ? List.of() : List.copyOf(selectedCardIds);
        cardFilter = cardFilter == null ? CardFilterSpec.any() : cardFilter;
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
    }

    public EffectDefinition(EffectType type, EffectTiming timing, EffectTarget target, int amount, SpecialCondition condition, List<EffectDefinition> children, EffectDefinition headsEffect, EffectDefinition tailsEffect, List<CardInstanceId> selectedCardIds) {
        this(type, timing, target, amount, condition, children, headsEffect, tailsEffect, selectedCardIds, null, null, CardFilterSpec.any(), -1, -1, -1, false, false);
    }

    public static EffectDefinition dealDamage(EffectTarget target, int amount, EffectTiming timing) {
        return new EffectDefinition(EffectType.DEAL_DAMAGE, timing, target, amount, null, List.of(), null, null, List.of());
    }

    public static EffectDefinition healDamage(EffectTarget target, int amount, EffectTiming timing) {
        return new EffectDefinition(EffectType.HEAL_DAMAGE, timing, target, amount, null, List.of(), null, null, List.of());
    }

    public static EffectDefinition applySpecialCondition(EffectTarget target, SpecialCondition condition, EffectTiming timing) {
        return new EffectDefinition(EffectType.APPLY_SPECIAL_CONDITION, timing, target, 0, condition, List.of(), null, null, List.of());
    }

    public static EffectDefinition drawCards(EffectTarget target, int count, EffectTiming timing) {
        return new EffectDefinition(EffectType.DRAW_CARDS, timing, target, count, null, List.of(), null, null, List.of());
    }

    public static EffectDefinition discardAttachedEnergy(EffectTarget target, int count, List<CardInstanceId> selectedEnergyIds, EffectTiming timing) {
        return new EffectDefinition(EffectType.DISCARD_ATTACHED_ENERGY, timing, target, count, null, List.of(), null, null, selectedEnergyIds);
    }

    public static EffectDefinition coinFlip(EffectDefinition headsEffect, EffectDefinition tailsEffect, EffectTiming timing) {
        return new EffectDefinition(EffectType.COIN_FLIP, timing, EffectTarget.ACTING_PLAYER, 0, null, List.of(), headsEffect, tailsEffect, List.of());
    }

    public static EffectDefinition composite(List<EffectDefinition> children, EffectTiming timing) {
        return new EffectDefinition(EffectType.COMPOSITE, timing, EffectTarget.ACTING_PLAYER, 0, null, children, null, null, List.of());
    }

    public static EffectDefinition searchDeck(EffectTarget target, int maxCount, CardFilterSpec filter, List<CardInstanceId> selectedCardIds, boolean revealSelectedCards, boolean requiresShuffle, EffectTiming timing) {
        return new EffectDefinition(EffectType.SEARCH_DECK, timing, target, maxCount, null, List.of(), null, null, selectedCardIds, EffectCardZone.DECK, EffectCardZone.HAND, filter, -1, -1, -1, revealSelectedCards, requiresShuffle);
    }

    public static EffectDefinition shuffleDeck(EffectTarget target, EffectTiming timing) {
        return new EffectDefinition(EffectType.SHUFFLE_DECK, timing, target, 0, null, List.of(), null, null, List.of(), EffectCardZone.DECK, EffectCardZone.DECK, CardFilterSpec.any(), -1, -1, -1, false, false);
    }

    public static EffectDefinition discardCards(EffectTarget target, EffectCardZone sourceZone, int count, List<CardInstanceId> selectedCardIds, EffectTiming timing) {
        return new EffectDefinition(EffectType.DISCARD_CARDS, timing, target, count, null, List.of(), null, null, selectedCardIds, sourceZone, EffectCardZone.DISCARD, CardFilterSpec.any(), -1, -1, -1, false, false);
    }

    public static EffectDefinition attachEnergy(EffectTarget target, EffectCardZone sourceZone, List<CardInstanceId> selectedEnergyIds, int targetBenchIndex, EffectTiming timing) {
        return new EffectDefinition(EffectType.ATTACH_ENERGY, timing, target, selectedEnergyIds == null || selectedEnergyIds.isEmpty() ? 1 : selectedEnergyIds.size(), null, List.of(), null, null, selectedEnergyIds, sourceZone, EffectCardZone.ATTACHED, CardFilterSpec.supertype(com.tpi.pokemon.game.domain.enums.CardSupertype.ENERGY), targetBenchIndex, -1, -1, false, false);
    }

    public static EffectDefinition moveEnergy(EffectTarget target, List<CardInstanceId> selectedEnergyIds, int sourceBenchIndex, int destinationBenchIndex, EffectTiming timing) {
        return new EffectDefinition(EffectType.MOVE_ENERGY, timing, target, selectedEnergyIds == null || selectedEnergyIds.isEmpty() ? 1 : selectedEnergyIds.size(), null, List.of(), null, null, selectedEnergyIds, EffectCardZone.ATTACHED, EffectCardZone.ATTACHED, CardFilterSpec.supertype(com.tpi.pokemon.game.domain.enums.CardSupertype.ENERGY), -1, sourceBenchIndex, destinationBenchIndex, false, false);
    }

    public static EffectDefinition switchActive(EffectTarget target, int targetBenchIndex, EffectTiming timing) {
        return new EffectDefinition(EffectType.SWITCH_ACTIVE, timing, target, 1, null, List.of(), null, null, List.of(), null, null, CardFilterSpec.any(), targetBenchIndex, -1, -1, false, false);
    }

    public static EffectDefinition placeDamageCounters(EffectTarget target, int counters, int targetBenchIndex, EffectTiming timing) {
        return new EffectDefinition(EffectType.PLACE_DAMAGE_COUNTERS, timing, target, counters, null, List.of(), null, null, List.of(), null, null, CardFilterSpec.any(), targetBenchIndex, -1, -1, false, false);
    }
}
