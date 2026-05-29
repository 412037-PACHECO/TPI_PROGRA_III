package com.tpi.pokemon.game.engine.knockout;

import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.List;
import java.util.Objects;

public record KnockoutResult(PlayerId ownerId, PokemonInPlay knockedOutPokemon, List<CardInstance> discardedCards, int prizeValue) {
    public KnockoutResult {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(knockedOutPokemon, "knockedOutPokemon must not be null");
        Objects.requireNonNull(discardedCards, "discardedCards must not be null");
        if (discardedCards.isEmpty()) {
            throw new IllegalArgumentException("discardedCards must not be empty");
        }
        if (prizeValue <= 0) {
            throw new IllegalArgumentException("prizeValue must be positive");
        }
        discardedCards = List.copyOf(discardedCards);
    }
}
