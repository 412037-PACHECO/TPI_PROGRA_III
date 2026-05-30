package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;

public record HealEffectResolvedEvent(GameId gameId, PlayerId ownerId, CardInstanceId pokemonId, int requestedAmount, int actualHealedAmount, int resultingDamageCounters) implements GameEvent {}
