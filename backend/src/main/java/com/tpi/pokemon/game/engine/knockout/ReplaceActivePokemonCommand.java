package com.tpi.pokemon.game.engine.knockout;

import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record ReplaceActivePokemonCommand(PlayerId playerId, int benchIndex) {
    public ReplaceActivePokemonCommand {
        Objects.requireNonNull(playerId, "playerId must not be null");
        if (benchIndex < 0) {
            throw new IllegalArgumentException("benchIndex must not be negative");
        }
    }
}
