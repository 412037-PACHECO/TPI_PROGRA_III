package com.tpi.pokemon.game.engine.effect.reactive;

import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record DamageSource(
        DamageSourceType type,
        PlayerId sourcePlayerId,
        PokemonInPlay sourcePokemon,
        String sourceId
) {
    public DamageSource {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(sourcePlayerId, "sourcePlayerId must not be null");
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("sourceId must not be blank");
        }
    }

    public static DamageSource attack(PlayerId attackerId, PokemonInPlay attacker, String attackId) {
        Objects.requireNonNull(attacker, "attacker must not be null");
        return new DamageSource(DamageSourceType.ATTACK, attackerId, attacker, attackId);
    }
}
