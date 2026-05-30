package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import java.util.List;
import java.util.Set;

final class EffectCardMovementSupport {
    private EffectCardMovementSupport() {}

    static List<CardInstance> selectedFrom(List<CardInstance> cards, List<CardInstanceId> selectedIds, String zoneName) {
        Set<CardInstanceId> selected = Set.copyOf(selectedIds);
        List<CardInstance> found = cards.stream().filter(card -> selected.contains(card.id())).toList();
        if (found.size() != selected.size()) {
            throw new EffectException("Selected card not found in " + zoneName);
        }
        return found;
    }

    static List<CardInstance> withoutSelected(List<CardInstance> cards, List<CardInstanceId> selectedIds) {
        Set<CardInstanceId> selected = Set.copyOf(selectedIds);
        return cards.stream().filter(card -> !selected.contains(card.id())).toList();
    }

    static void requireMatchesFilter(List<CardInstance> cards, CardFilterSpec filter) {
        for (CardInstance card : cards) {
            if (!filter.matches(card)) {
                throw new EffectException("Selected card " + card.id().value() + " does not match effect filter");
            }
        }
    }
}
