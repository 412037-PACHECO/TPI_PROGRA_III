package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;

public record ActivePokemonSwitchedEvent(GameId gameId, PlayerId playerId, CardInstanceId oldActivePokemonId, CardInstanceId newActivePokemonId) implements GameEvent {}
