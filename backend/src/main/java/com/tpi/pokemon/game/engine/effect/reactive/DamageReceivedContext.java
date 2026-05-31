package com.tpi.pokemon.game.engine.effect.reactive;

import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record DamageReceivedContext(
        GameState state,
        PlayerId damagedPlayerId,
        PokemonInPlay damagedPokemon,
        int damage,
        DamageSource source
) {
    public DamageReceivedContext {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(damagedPlayerId, "damagedPlayerId must not be null");
        Objects.requireNonNull(damagedPokemon, "damagedPokemon must not be null");
        if (damage < 0) {
            throw new IllegalArgumentException("damage must not be negative");
        }
        Objects.requireNonNull(source, "source must not be null");
    }
}
