package com.tpi.pokemon.game.engine.attack;

import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.command.PlayerCommand;

public record DeclareAttackCommand(PlayerId playerId, String attackId) implements PlayerCommand {
    public DeclareAttackCommand {
        if (playerId == null) {
            throw new IllegalArgumentException("playerId must not be null");
        }
        if (attackId == null || attackId.isBlank()) {
            throw new IllegalArgumentException("attackId must not be blank");
        }
    }
}
