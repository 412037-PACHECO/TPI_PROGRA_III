package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.List;

public record EnergyMovedEvent(GameId gameId, PlayerId playerId, List<CardInstanceId> energyCardIds, CardInstanceId sourcePokemonId, CardInstanceId destinationPokemonId) implements GameEvent {
    public EnergyMovedEvent {
        energyCardIds = List.copyOf(energyCardIds);
    }
}
