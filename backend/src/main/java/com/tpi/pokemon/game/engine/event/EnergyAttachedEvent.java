package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;

public record EnergyAttachedEvent(GameId gameId, PlayerId playerId, CardInstanceId energyCardId, CardInstanceId targetPokemonId) implements GameEvent {}
