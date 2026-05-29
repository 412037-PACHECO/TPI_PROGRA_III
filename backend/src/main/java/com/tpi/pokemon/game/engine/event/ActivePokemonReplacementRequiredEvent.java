package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;

public record ActivePokemonReplacementRequiredEvent(GameId gameId, PlayerId playerId, String reason) implements GameEvent {}
