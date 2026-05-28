package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record CardInstance(CardInstanceId id, CardDefinitionRef definition, PlayerId owner) {
    public CardInstance {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(definition, "definition must not be null");
        Objects.requireNonNull(owner, "owner must not be null");
    }
}
