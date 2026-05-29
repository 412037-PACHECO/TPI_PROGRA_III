package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.random.CoinFlipResult;

public record ConfusionCheckResolvedEvent(GameId gameId, PlayerId playerId, CardInstanceId pokemonId, String attackId, CoinFlipResult result) implements GameEvent {}
