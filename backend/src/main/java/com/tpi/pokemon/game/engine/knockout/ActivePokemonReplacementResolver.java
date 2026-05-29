package com.tpi.pokemon.game.engine.knockout;

import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.model.ActivePokemon;
import com.tpi.pokemon.game.domain.model.Bench;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.engine.event.ActivePokemonReplacedEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.turn.EndTurnCommand;
import com.tpi.pokemon.game.engine.turn.TurnManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ActivePokemonReplacementResolver {
    private final TurnManager turnManager;

    public ActivePokemonReplacementResolver() {
        this(new TurnManager());
    }

    public ActivePokemonReplacementResolver(TurnManager turnManager) {
        this.turnManager = Objects.requireNonNull(turnManager, "turnManager must not be null");
    }

    public GameState replaceActive(GameState state, ReplaceActivePokemonCommand command) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(command, "command must not be null");
        if (state.getStatus() != GameStatus.ACTIVE) {
            throw new KnockoutException("Game must be ACTIVE");
        }
        PendingActiveReplacement pending = state.getPendingActiveReplacement()
                .orElseThrow(() -> new KnockoutException("No active Pokemon replacement is pending"));
        if (!pending.playerId().equals(command.playerId())) {
            throw new KnockoutException("Only the player with pending replacement can choose a new active Pokemon");
        }
        PlayerGameState player = playerState(state, command.playerId());
        if (player.getBoard().getActivePokemon().isPresent()) {
            throw new KnockoutException("Player already has an active Pokemon");
        }
        if (command.benchIndex() >= player.getBoard().getBench().getPokemon().size()) {
            throw new KnockoutException("Bench index is out of range");
        }

        PokemonInPlay newActive = player.getBoard().getBench().getPokemon().get(command.benchIndex());
        Bench updatedBench = player.getBoard().getBench().withoutPokemonAt(command.benchIndex());
        BoardState updatedBoard = new BoardState(new ActivePokemon(newActive), updatedBench, player.getBoard().getActiveStadium().orElse(null));
        PlayerGameState updatedPlayer = player.withBoard(updatedBoard);

        List<GameEvent> events = new ArrayList<>(state.getEvents());
        events.add(new ActivePokemonReplacedEvent(state.getGameId(), command.playerId(), newActive.getTopCard().id()));
        GameState replaced = new GameState(
                state.getGameId(),
                GameStatus.ACTIVE,
                state.getPlayerOneState().getPlayerId().equals(command.playerId()) ? updatedPlayer : state.getPlayerOneState(),
                state.getPlayerTwoState().getPlayerId().equals(command.playerId()) ? updatedPlayer : state.getPlayerTwoState(),
                state.getTurnState(),
                state.getActiveStadium().orElse(null),
                null,
                null,
                events
        );
        return turnManager.endTurn(replaced, new EndTurnCommand(state.getTurnState().currentPlayer()));
    }

    private PlayerGameState playerState(GameState state, com.tpi.pokemon.game.domain.value.PlayerId playerId) {
        if (state.getPlayerOneState().getPlayerId().equals(playerId)) {
            return state.getPlayerOneState();
        }
        if (state.getPlayerTwoState().getPlayerId().equals(playerId)) {
            return state.getPlayerTwoState();
        }
        throw new KnockoutException("Player is not part of this game");
    }
}
