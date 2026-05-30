package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.enums.EnergyType;
import com.tpi.pokemon.game.domain.model.CardInstance;
import java.util.Objects;

public record CardFilterSpec(CardSupertype supertype, CardSubtype subtype, EnergyType energyType) {
    public static CardFilterSpec any() {
        return new CardFilterSpec(null, null, null);
    }

    public static CardFilterSpec supertype(CardSupertype supertype) {
        return new CardFilterSpec(Objects.requireNonNull(supertype, "supertype must not be null"), null, null);
    }

    public static CardFilterSpec subtype(CardSubtype subtype) {
        return new CardFilterSpec(null, Objects.requireNonNull(subtype, "subtype must not be null"), null);
    }

    public static CardFilterSpec energyType(EnergyType energyType) {
        return new CardFilterSpec(CardSupertype.ENERGY, null, Objects.requireNonNull(energyType, "energyType must not be null"));
    }

    public boolean matches(CardInstance card) {
        Objects.requireNonNull(card, "card must not be null");
        if (supertype != null && card.definition().supertype() != supertype) {
            return false;
        }
        if (subtype != null && !card.definition().subtypes().contains(subtype)) {
            return false;
        }
        return energyType == null || card.definition().energyProfile().provides().contains(energyType);
    }
}
