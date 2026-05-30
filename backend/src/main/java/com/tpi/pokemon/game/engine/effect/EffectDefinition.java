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
        List<CardInstanceId> selectedCardIds
) {
    public EffectDefinition {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(timing, "timing must not be null");
        children = children == null ? List.of() : List.copyOf(children);
        selectedCardIds = selectedCardIds == null ? List.of() : List.copyOf(selectedCardIds);
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
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
}
