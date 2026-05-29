package com.tpi.pokemon.game.engine.knockout;

import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record PendingActiveReplacement(PlayerId playerId, ActiveReplacementReason reason) {
    public PendingActiveReplacement {
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
    }
}
