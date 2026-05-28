package com.tpi.pokemon.game.engine.action;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.List;

public record RetreatActivePokemonCommand(PlayerId playerId, int benchIndex, List<CardInstanceId> energyCardsToDiscard) {}
