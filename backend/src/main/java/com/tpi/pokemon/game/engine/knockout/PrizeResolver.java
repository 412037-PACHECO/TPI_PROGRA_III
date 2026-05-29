package com.tpi.pokemon.game.engine.knockout;

import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PrizeCards;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.PrizeCardsTakenEvent;
import java.util.List;

public final class PrizeResolver {
    public PrizeResolution takePrizes(GameState state, PlayerId prizeTakerId, int requestedPrizeCount, List<GameEvent> events) {
        PlayerGameState prizeTaker = playerState(state, prizeTakerId);
        PrizeCards.PrizeDraw draw = prizeTaker.getPrizeCards().drawUpTo(requestedPrizeCount);
        PlayerGameState updatedPrizeTaker = prizeTaker
                .withPrizeCards(draw.remainingPrizeCards())
                .withHand(prizeTaker.getHand().withCardsAdded(draw.takenCards()));
        events.add(new PrizeCardsTakenEvent(state.getGameId(), prizeTakerId, draw.takenCards().stream().map(card -> card.id()).toList(), draw.remainingPrizeCards().remainingCount()));
        return new PrizeResolution(withPlayer(state, updatedPrizeTaker, events), new PrizeTakenResult(prizeTakerId, draw.takenCards(), draw.remainingPrizeCards().remainingCount()));
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

    private GameState withPlayer(GameState state, PlayerGameState updatedPlayer, List<GameEvent> events) {
        PlayerGameState playerOne = state.getPlayerOneState().getPlayerId().equals(updatedPlayer.getPlayerId()) ? updatedPlayer : state.getPlayerOneState();
        PlayerGameState playerTwo = state.getPlayerTwoState().getPlayerId().equals(updatedPlayer.getPlayerId()) ? updatedPlayer : state.getPlayerTwoState();
        return new GameState(state.getGameId(), state.getStatus(), playerOne, playerTwo, state.getTurnState(), state.getActiveStadium().orElse(null), state.getFinishResult().orElse(null), state.getPendingActiveReplacement().orElse(null), events);
    }

    public record PrizeResolution(GameState state, PrizeTakenResult prizeTakenResult) {}
}
