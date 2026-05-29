package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;

public record SpecialConditionDamageAppliedEvent(GameId gameId, PlayerId ownerId, CardInstanceId pokemonId, SpecialCondition condition, int damageAmount, int resultingDamageCounters) implements GameEvent {}
