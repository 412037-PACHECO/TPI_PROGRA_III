package com.tpi.pokemon.game.engine.victory;

import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.List;
import java.util.Optional;

public final class VictoryConditionChecker {
    public Optional<GameFinishResult> checkAfterKnockout(GameState state, PlayerId potentialWinnerId, PlayerId potentialLoserId) {
        PlayerGameState winner = playerState(state, potentialWinnerId);
        PlayerGameState loser = playerState(state, potentialLoserId);

        java.util.ArrayList<FinishReason> reasons = new java.util.ArrayList<>();
        if (winner.getPrizeCards().isEmpty()) {
            reasons.add(FinishReason.PRIZES_TAKEN);
        }
        if (!loser.hasPokemonInPlay()) {
            reasons.add(FinishReason.OPPONENT_HAS_NO_POKEMON_IN_PLAY);
        }
        if (reasons.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(GameFinishResult.singleWinner(potentialWinnerId, potentialLoserId, reasons));
    }

    public GameFinishResult deckOut(PlayerId loserId, PlayerId winnerId) {
        return GameFinishResult.singleWinner(winnerId, loserId, FinishReason.DECK_OUT);
    }

    public GameFinishResult suddenDeathRequired(List<FinishReason> reasons) {
        return GameFinishResult.suddenDeathRequired(reasons);
    }

    private PlayerGameState playerState(GameState state, PlayerId playerId) {
        if (state.getPlayerOneState().getPlayerId().equals(playerId)) {
            return state.getPlayerOneState();
        }
        if (state.getPlayerTwoState().getPlayerId().equals(playerId)) {
            return state.getPlayerTwoState();
        }
        throw new IllegalArgumentException("Player is not part of this game");
    }
}
