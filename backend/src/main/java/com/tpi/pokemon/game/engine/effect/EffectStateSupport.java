package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.ActivePokemon;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.DiscardPile;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;

final class EffectStateSupport {
    private EffectStateSupport() {}

    static PlayerId ownerFor(EffectExecutionContext context, EffectTarget target) {
        return target == EffectTarget.DEFENDER_ACTIVE ? context.defendingPlayerId() : context.actingPlayerId();
    }

    static PlayerGameState playerState(GameState state, PlayerId playerId) {
        if (state.getPlayerOneState().getPlayerId().equals(playerId)) return state.getPlayerOneState();
        if (state.getPlayerTwoState().getPlayerId().equals(playerId)) return state.getPlayerTwoState();
        throw new EffectException("Player is not part of this game");
    }

    static PokemonInPlay activePokemon(GameState state, PlayerId ownerId) {
        return playerState(state, ownerId).getBoard().getActivePokemon()
                .map(ActivePokemon::getPokemon)
                .orElseThrow(() -> new EffectException("Target player has no active Pokemon"));
    }

    static GameState withActivePokemon(GameState state, PlayerId ownerId, PokemonInPlay pokemon) {
        PlayerGameState owner = playerState(state, ownerId);
        BoardState updatedBoard = new BoardState(new ActivePokemon(pokemon), owner.getBoard().getBench(), owner.getBoard().getActiveStadium().orElse(null));
        return withPlayer(state, new PlayerGameState(owner.getPlayerId(), owner.getDeck(), owner.getHand(), owner.getPrizeCards(), owner.getDiscardPile(), updatedBoard, owner.getTurnsTaken()));
    }

    static GameState withPlayer(GameState state, PlayerGameState updatedPlayer) {
        PlayerGameState playerOne = state.getPlayerOneState().getPlayerId().equals(updatedPlayer.getPlayerId()) ? updatedPlayer : state.getPlayerOneState();
        PlayerGameState playerTwo = state.getPlayerTwoState().getPlayerId().equals(updatedPlayer.getPlayerId()) ? updatedPlayer : state.getPlayerTwoState();
        return new GameState(state.getGameId(), state.getStatus(), playerOne, playerTwo, state.getTurnState(), state.getActiveStadium().orElse(null), state.getFinishResult().orElse(null), state.getPendingActiveReplacement().orElse(null), state.getEvents());
    }

    static PlayerGameState withDeckAndHand(PlayerGameState player, DeckZone deck, HandZone hand) {
        return new PlayerGameState(player.getPlayerId(), deck, hand, player.getPrizeCards(), player.getDiscardPile(), player.getBoard(), player.getTurnsTaken());
    }

    static PlayerGameState withDiscardAndBoard(PlayerGameState player, DiscardPile discardPile, BoardState board) {
        return new PlayerGameState(player.getPlayerId(), player.getDeck(), player.getHand(), player.getPrizeCards(), discardPile, board, player.getTurnsTaken());
    }
}
