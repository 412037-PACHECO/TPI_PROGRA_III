package com.tpi.pokemon.game.engine.attack;

import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.command.PlayerCommand;

public record DeclareAttackCommand(GameId gameId, PlayerId playerId, String attackId) implements PlayerCommand {
    public DeclareAttackCommand {
        if (gameId == null) {
            throw new IllegalArgumentException("gameId must not be null");
        }
        if (playerId == null) {
            throw new IllegalArgumentException("playerId must not be null");
        }
        if (attackId == null || attackId.isBlank()) {
            throw new IllegalArgumentException("attackId must not be blank");
        }
    }
}
