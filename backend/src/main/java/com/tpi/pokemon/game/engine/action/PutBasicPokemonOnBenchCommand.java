package com.tpi.pokemon.game.engine.action;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;

public record PutBasicPokemonOnBenchCommand(PlayerId playerId, CardInstanceId cardId) {}
