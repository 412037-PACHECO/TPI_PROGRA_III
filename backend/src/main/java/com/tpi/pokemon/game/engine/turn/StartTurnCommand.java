package com.tpi.pokemon.game.engine.turn;

import com.tpi.pokemon.game.domain.value.PlayerId;

public record StartTurnCommand(PlayerId playerId) {}
