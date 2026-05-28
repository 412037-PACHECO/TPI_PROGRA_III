package com.tpi.pokemon.game.engine.random;

import com.tpi.pokemon.game.domain.value.PlayerId;

public interface StartingPlayerSelector {
    PlayerId selectStartingPlayer(PlayerId playerOne, PlayerId playerTwo);
}
