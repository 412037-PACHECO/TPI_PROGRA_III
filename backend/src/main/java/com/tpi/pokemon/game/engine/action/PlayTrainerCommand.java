package com.tpi.pokemon.game.engine.action;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Optional;
import java.util.Objects;

public record PlayTrainerCommand(PlayerId playerId, CardInstanceId trainerCardId, Optional<PokemonTarget> target) {
    public PlayTrainerCommand {
        target = Objects.requireNonNullElse(target, Optional.empty());
    }

    public PlayTrainerCommand(PlayerId playerId, CardInstanceId trainerCardId) {
        this(playerId, trainerCardId, Optional.empty());
    }
}
