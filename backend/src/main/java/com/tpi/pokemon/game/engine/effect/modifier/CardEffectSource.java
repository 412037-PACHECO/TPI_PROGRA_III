package com.tpi.pokemon.game.engine.effect.modifier;

import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.effect.ability.EffectSourceKind;
import java.util.Objects;
import java.util.Optional;

public record CardEffectSource(CardInstance card, PlayerId owner, EffectSourceKind kind, boolean activePokemon, PokemonInPlay sourcePokemonInPlay, PokemonInPlay attachedPokemon) {
    public CardEffectSource {
        Objects.requireNonNull(card, "card must not be null");
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
    }

    public Optional<PokemonInPlay> sourcePokemon() {
        return Optional.ofNullable(sourcePokemonInPlay);
    }

    public Optional<PokemonInPlay> attachedTo() {
        return Optional.ofNullable(attachedPokemon);
    }
}
