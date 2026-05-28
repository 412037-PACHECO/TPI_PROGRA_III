package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.GameCreatedEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import java.util.List;
import java.util.Objects;

public final class GameState {
    private final GameId gameId;
    private final GameStatus status;
    private final PlayerGameState playerOneState;
    private final PlayerGameState playerTwoState;
    private final TurnState turnState;
    private final List<GameEvent> events;

    public GameState(GameId gameId, GameStatus status, PlayerGameState playerOneState, PlayerGameState playerTwoState, TurnState turnState, List<GameEvent> events) {
        this.gameId = Objects.requireNonNull(gameId, "gameId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.playerOneState = Objects.requireNonNull(playerOneState, "playerOneState must not be null");
        this.playerTwoState = Objects.requireNonNull(playerTwoState, "playerTwoState must not be null");
        this.turnState = Objects.requireNonNull(turnState, "turnState must not be null");
        Objects.requireNonNull(events, "events must not be null");
        if (events.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("events must not contain null values");
        }
        if (playerOneState.getPlayerId().equals(playerTwoState.getPlayerId())) {
            throw new IllegalArgumentException("Game players must be distinct");
        }
        this.events = List.copyOf(events);
    }

    public static GameState created(GameId gameId, PlayerId playerOneId, PlayerId playerTwoId) {
        Objects.requireNonNull(playerOneId, "playerOneId must not be null");
        Objects.requireNonNull(playerTwoId, "playerTwoId must not be null");
        if (playerOneId.equals(playerTwoId)) {
            throw new IllegalArgumentException("Game players must be distinct");
        }
        return new GameState(
                gameId,
                GameStatus.CREATED,
                PlayerGameState.empty(playerOneId),
                PlayerGameState.empty(playerTwoId),
                TurnState.notStarted(),
                List.of(new GameCreatedEvent(gameId, playerOneId, playerTwoId))
        );
    }

    public GameId getGameId() {
        return gameId;
    }

    public GameStatus getStatus() {
        return status;
    }

    public PlayerGameState getPlayerOneState() {
        return playerOneState;
    }

    public PlayerGameState getPlayerTwoState() {
        return playerTwoState;
    }

    public TurnState getTurnState() {
        return turnState;
    }

    public List<GameEvent> getEvents() {
        return events;
    }
}
