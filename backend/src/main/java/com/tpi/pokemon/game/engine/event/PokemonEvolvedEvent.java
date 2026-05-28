package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;

public record PokemonEvolvedEvent(GameId gameId, PlayerId playerId, CardInstanceId evolutionCardId, CardInstanceId evolvedFromCardId) implements GameEvent {}
