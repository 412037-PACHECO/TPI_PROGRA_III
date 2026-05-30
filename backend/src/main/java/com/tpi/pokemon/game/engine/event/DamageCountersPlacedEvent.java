package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;

public record DamageCountersPlacedEvent(GameId gameId, PlayerId actingPlayerId, CardInstanceId pokemonId, int countersPlaced, int resultingDamageCounters, String sourceId) implements GameEvent {}
