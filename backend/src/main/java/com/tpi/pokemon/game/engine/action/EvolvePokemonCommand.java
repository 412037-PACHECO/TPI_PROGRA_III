package com.tpi.pokemon.game.engine.action;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;

public record EvolvePokemonCommand(PlayerId playerId, CardInstanceId evolutionCardId, PokemonTarget target) {}
