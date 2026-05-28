package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.List;

public record ActivePokemonRetreatedEvent(GameId gameId, PlayerId playerId, CardInstanceId newActivePokemonId, List<CardInstanceId> discardedEnergyIds) implements GameEvent {}
