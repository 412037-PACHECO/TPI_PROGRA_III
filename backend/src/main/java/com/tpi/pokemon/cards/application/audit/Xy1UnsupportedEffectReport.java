package com.tpi.pokemon.cards.application.audit;

import com.tpi.pokemon.game.engine.effect.mapping.Xy1EffectCategory;

public record Xy1UnsupportedEffectReport(
        String cardId,
        String cardName,
        String sourceType,
        String sourceName,
        Xy1EffectCategory category,
        String reason
) {}
