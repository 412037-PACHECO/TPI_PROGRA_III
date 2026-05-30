package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.effect.EffectCardZone;
import com.tpi.pokemon.game.engine.effect.EffectTarget;
import com.tpi.pokemon.game.engine.effect.EffectType;

public record PendingSelectionRequiredEvent(GameId gameId, PlayerId playerId, EffectType effectType, String sourceId, EffectCardZone sourceZone, EffectTarget target, int minSelections, int maxSelections) implements GameEvent {}
