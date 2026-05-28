package com.tpi.pokemon.game.engine.setup;

import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.value.PlayerId;

public interface MulliganBonusDrawPolicy {
    int cardsToDraw(PlayerId playerId, int opponentMulligans, GameState state);
}
