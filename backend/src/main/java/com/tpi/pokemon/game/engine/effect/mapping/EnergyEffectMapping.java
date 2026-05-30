package com.tpi.pokemon.game.engine.effect.mapping;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.model.EnergyProfile;
import com.tpi.pokemon.game.engine.effect.EffectDefinition;
import com.tpi.pokemon.game.engine.effect.ability.CardEffectDefinition;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record EnergyEffectMapping(
        String cardId,
        String cardName,
        CardSubtype subtype,
        String ruleText,
        Set<Xy1EffectCategory> categories,
        Xy1EffectComplexity complexity,
        EnergyProfile energyProfile,
        List<EffectDefinition> playEffects,
        List<CardEffectDefinition> continuousEffects,
        Set<Xy1AuditStatus> statuses,
        boolean tested,
        String notes
) {
    public EnergyEffectMapping {
        if (cardId == null || cardId.isBlank()) {
            throw new IllegalArgumentException("cardId must not be blank");
        }
        if (cardName == null || cardName.isBlank()) {
            throw new IllegalArgumentException("cardName must not be blank");
        }
        Objects.requireNonNull(subtype, "subtype must not be null");
        ruleText = ruleText == null ? "" : ruleText;
        categories = Set.copyOf(Objects.requireNonNull(categories, "categories must not be null"));
        Objects.requireNonNull(complexity, "complexity must not be null");
        Objects.requireNonNull(energyProfile, "energyProfile must not be null");
        playEffects = List.copyOf(Objects.requireNonNull(playEffects, "playEffects must not be null"));
        continuousEffects = List.copyOf(Objects.requireNonNull(continuousEffects, "continuousEffects must not be null"));
        statuses = Set.copyOf(Objects.requireNonNull(statuses, "statuses must not be null"));
        notes = notes == null ? "" : notes;
    }
}
