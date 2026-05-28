package com.tpi.pokemon.game.engine.setup;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.List;
import java.util.Objects;

public record ChooseInitialPokemonCommand(PlayerId playerId, CardInstanceId activePokemonId, List<CardInstanceId> benchPokemonIds) {
    public ChooseInitialPokemonCommand {
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(activePokemonId, "activePokemonId must not be null");
        Objects.requireNonNull(benchPokemonIds, "benchPokemonIds must not be null");
        if (benchPokemonIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("benchPokemonIds must not contain null values");
        }
        benchPokemonIds = List.copyOf(benchPokemonIds);
    }
}
