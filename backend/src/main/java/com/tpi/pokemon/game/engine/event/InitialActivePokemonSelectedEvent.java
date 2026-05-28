package com.tpi.pokemon.game.engine.event;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Objects;

public record InitialActivePokemonSelectedEvent(GameId gameId, PlayerId playerId, CardInstanceId cardId) implements GameEvent {
    public InitialActivePokemonSelectedEvent {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(cardId, "cardId must not be null");
    }
}
