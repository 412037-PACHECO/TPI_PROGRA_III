package com.tpi.pokemon.game.engine.effect.mapping;

import com.tpi.pokemon.game.engine.effect.EffectDefinition;
import com.tpi.pokemon.game.engine.effect.ability.CardEffectDefinition;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record TrainerEffectMapping(
        String cardId,
        String cardName,
        String subtype,
        String effectText,
        Set<Xy1EffectCategory> categories,
        Xy1EffectComplexity complexity,
        List<EffectDefinition> playEffects,
        List<CardEffectDefinition> continuousEffects,
        Set<Xy1AuditStatus> statuses,
        boolean tested,
        String notes
) {
    public TrainerEffectMapping {
        if (cardId == null || cardId.isBlank()) {
            throw new IllegalArgumentException("cardId must not be blank");
        }
        if (cardName == null || cardName.isBlank()) {
            throw new IllegalArgumentException("cardName must not be blank");
        }
        if (subtype == null || subtype.isBlank()) {
            throw new IllegalArgumentException("subtype must not be blank");
        }
        effectText = effectText == null ? "" : effectText;
        Objects.requireNonNull(complexity, "complexity must not be null");
        categories = Set.copyOf(Objects.requireNonNull(categories, "categories must not be null"));
        playEffects = List.copyOf(Objects.requireNonNull(playEffects, "playEffects must not be null"));
        continuousEffects = List.copyOf(Objects.requireNonNull(continuousEffects, "continuousEffects must not be null"));
        statuses = Set.copyOf(Objects.requireNonNull(statuses, "statuses must not be null"));
        notes = notes == null ? "" : notes;
    }
}
