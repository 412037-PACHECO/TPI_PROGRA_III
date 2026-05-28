package com.tpi.pokemon.game.engine.command;

import com.tpi.pokemon.game.domain.value.PlayerId;

public interface PlayerCommand extends GameCommand {
    PlayerId playerId();
}
