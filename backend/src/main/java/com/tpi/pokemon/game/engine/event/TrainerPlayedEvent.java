package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;

public record TrainerPlayedEvent(GameId gameId, PlayerId playerId, CardInstanceId trainerCardId, String trainerType) implements GameEvent {}
