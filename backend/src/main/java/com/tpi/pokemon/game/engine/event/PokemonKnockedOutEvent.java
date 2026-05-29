package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;

public record PokemonKnockedOutEvent(GameId gameId, PlayerId ownerId, CardInstanceId pokemonId, int damageCounters, int hp) implements GameEvent {}
