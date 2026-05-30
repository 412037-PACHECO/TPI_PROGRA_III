package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.effect.EffectType;

public record EffectResolvedEvent(GameId gameId, PlayerId playerId, EffectType effectType, String sourceId) implements GameEvent {}
