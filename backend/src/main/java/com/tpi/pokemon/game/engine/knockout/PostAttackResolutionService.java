package com.tpi.pokemon.game.engine.knockout;

import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.ActivePokemonReplacementRequiredEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.GameFinishedEvent;
import com.tpi.pokemon.game.engine.event.VictoryDetectedEvent;
import com.tpi.pokemon.game.engine.turn.EndTurnCommand;
import com.tpi.pokemon.game.engine.turn.TurnManager;
import com.tpi.pokemon.game.engine.victory.FinishReason;
import com.tpi.pokemon.game.engine.victory.GameFinishResult;
import com.tpi.pokemon.game.engine.victory.VictoryConditionChecker;
import java.util.List;
import java.util.Objects;

public final class PostAttackResolutionService {
    private final KnockoutResolver knockoutResolver;
    private final PrizeResolver prizeResolver;
    private final VictoryConditionChecker victoryConditionChecker;
    private final TurnManager turnManager;

    public PostAttackResolutionService() {
        this(new KnockoutResolver(), new PrizeResolver(), new VictoryConditionChecker(), new TurnManager());
    }

    public PostAttackResolutionService(KnockoutResolver knockoutResolver, PrizeResolver prizeResolver, VictoryConditionChecker victoryConditionChecker, TurnManager turnManager) {
        this.knockoutResolver = Objects.requireNonNull(knockoutResolver, "knockoutResolver must not be null");
        this.prizeResolver = Objects.requireNonNull(prizeResolver, "prizeResolver must not be null");
        this.victoryConditionChecker = Objects.requireNonNull(victoryConditionChecker, "victoryConditionChecker must not be null");
        this.turnManager = Objects.requireNonNull(turnManager, "turnManager must not be null");
    }

    public GameState resolveAfterAttack(GameState state, PlayerId attackerId, PlayerId defenderId, List<GameEvent> events) {
        return knockoutResolver.resolveActiveKnockout(state, defenderId, events)
                .map(resolution -> finishTurnIfReady(resolveKnockoutConsequences(resolution.state(), resolution.knockout(), attackerId, defenderId, events), attackerId))
                .orElseGet(() -> turnManager.endTurn(state, new EndTurnCommand(attackerId)));
    }

    public GameState resolveActiveKnockout(GameState state, PlayerId knockedOutOwnerId, PlayerId prizeTakerId, List<GameEvent> events) {
        return knockoutResolver.resolveActiveKnockout(state, knockedOutOwnerId, events)
                .map(resolution -> resolveKnockoutConsequences(resolution.state(), resolution.knockout(), prizeTakerId, knockedOutOwnerId, events))
                .orElse(state);
    }

    private GameState resolveKnockoutConsequences(GameState state, KnockoutResult knockout, PlayerId prizeTakerId, PlayerId knockedOutOwnerId, List<GameEvent> events) {
        PrizeResolver.PrizeResolution prizeResolution = prizeResolver.takePrizes(state, prizeTakerId, knockout.prizeValue(), events);
        GameState afterPrizes = prizeResolution.state();

        java.util.Optional<GameFinishResult> finishResult = victoryConditionChecker.checkAfterKnockout(afterPrizes, prizeTakerId, knockedOutOwnerId);
        if (finishResult.isPresent()) {
            GameFinishResult result = finishResult.get();
            FinishReason primaryReason = result.reasons().get(0);
            events.add(new VictoryDetectedEvent(afterPrizes.getGameId(), prizeTakerId, knockedOutOwnerId, primaryReason));
            events.add(new GameFinishedEvent(afterPrizes.getGameId(), prizeTakerId, knockedOutOwnerId, primaryReason));
            return new GameState(afterPrizes.getGameId(), GameStatus.FINISHED, afterPrizes.getPlayerOneState(), afterPrizes.getPlayerTwoState(), afterPrizes.getTurnState(), afterPrizes.getActiveStadium().orElse(null), result, null, events);
        }

        PlayerGameState knockedOutOwner = playerState(afterPrizes, knockedOutOwnerId);
        if (!knockedOutOwner.getBoard().getBench().isEmpty()) {
            PendingActiveReplacement pending = new PendingActiveReplacement(knockedOutOwnerId, ActiveReplacementReason.ACTIVE_KNOCKED_OUT);
            events.add(new ActivePokemonReplacementRequiredEvent(afterPrizes.getGameId(), knockedOutOwnerId, pending.reason().name()));
            return new GameState(afterPrizes.getGameId(), GameStatus.ACTIVE, afterPrizes.getPlayerOneState(), afterPrizes.getPlayerTwoState(), afterPrizes.getTurnState(), afterPrizes.getActiveStadium().orElse(null), null, pending, events);
        }

        return afterPrizes;
    }

    private GameState finishTurnIfReady(GameState state, PlayerId currentPlayerId) {
        if (state.getStatus() != GameStatus.ACTIVE || state.getPendingActiveReplacement().isPresent()) {
            return state;
        }
        return turnManager.endTurn(state, new EndTurnCommand(currentPlayerId));
    }

    private PlayerGameState playerState(GameState state, PlayerId playerId) {
        if (state.getPlayerOneState().getPlayerId().equals(playerId)) {
            return state.getPlayerOneState();
        }
        if (state.getPlayerTwoState().getPlayerId().equals(playerId)) {
            return state.getPlayerTwoState();
        }
        throw new KnockoutException("Player is not part of this game");
    }
}
