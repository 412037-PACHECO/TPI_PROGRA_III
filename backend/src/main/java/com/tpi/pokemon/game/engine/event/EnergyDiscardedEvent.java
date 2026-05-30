package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.List;

public record EnergyDiscardedEvent(GameId gameId, PlayerId ownerId, CardInstanceId pokemonId, List<CardInstanceId> energyCardIds) implements GameEvent {
    public EnergyDiscardedEvent { energyCardIds = List.copyOf(energyCardIds); }
}
