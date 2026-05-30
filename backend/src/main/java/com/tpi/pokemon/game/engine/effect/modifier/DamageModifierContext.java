package com.tpi.pokemon.game.engine.effect.modifier;

import com.tpi.pokemon.game.domain.model.AttackDefinition;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record DamageModifierContext(GameState state, PlayerId attackerPlayerId, PlayerId defenderPlayerId, PokemonInPlay attacker, PokemonInPlay defender, AttackDefinition attack) {
    public DamageModifierContext {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(attackerPlayerId, "attackerPlayerId must not be null");
        Objects.requireNonNull(defenderPlayerId, "defenderPlayerId must not be null");
        Objects.requireNonNull(attacker, "attacker must not be null");
        Objects.requireNonNull(defender, "defender must not be null");
        Objects.requireNonNull(attack, "attack must not be null");
    }
}
