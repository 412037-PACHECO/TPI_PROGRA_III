package com.tpi.pokemon.game.engine.knockout;

import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.model.ActivePokemon;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.ActivePokemonReplacementRequiredEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.GameFinishedEvent;
import com.tpi.pokemon.game.engine.event.SuddenDeathRequiredEvent;
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
        boolean defenderKnockedOut = activePokemon(state, defenderId).map(knockoutResolver::isKnockedOut).orElse(false);
        boolean attackerKnockedOut = activePokemon(state, attackerId).map(knockoutResolver::isKnockedOut).orElse(false);
        if (defenderKnockedOut && attackerKnockedOut) {
            return finishTurnIfReady(resolveSimultaneousActiveKnockouts(state, attackerId, defenderId, events), attackerId);
        }
        if (defenderKnockedOut) {
            return knockoutResolver.resolveActiveKnockout(state, defenderId, events)
                    .map(resolution -> finishTurnIfReady(resolveKnockoutConsequences(resolution.state(), resolution.knockout(), attackerId, defenderId, events), attackerId))
                    .orElseGet(() -> turnManager.endTurn(state, new EndTurnCommand(attackerId)));
        }
        if (attackerKnockedOut) {
            return knockoutResolver.resolveActiveKnockout(state, attackerId, events)
                    .map(resolution -> finishTurnIfReady(resolveKnockoutConsequences(resolution.state(), resolution.knockout(), defenderId, attackerId, events), attackerId))
                    .orElseGet(() -> turnManager.endTurn(state, new EndTurnCommand(attackerId)));
        }
        return turnManager.endTurn(state, new EndTurnCommand(attackerId));
    }

    public GameState resolveActiveKnockout(GameState state, PlayerId knockedOutOwnerId, PlayerId prizeTakerId, List<GameEvent> events) {
        return knockoutResolver.resolveActiveKnockout(state, knockedOutOwnerId, events)
                .map(resolution -> resolveKnockoutConsequences(resolution.state(), resolution.knockout(), prizeTakerId, knockedOutOwnerId, events))
                .orElse(state);
    }

    public GameState resolveBenchKnockout(GameState state, PlayerId knockedOutOwnerId, int benchIndex, PlayerId prizeTakerId, List<GameEvent> events) {
        return knockoutResolver.resolveBenchKnockout(state, knockedOutOwnerId, benchIndex, events)
                .map(resolution -> resolveKnockoutConsequencesWithoutActiveReplacement(resolution.state(), resolution.knockout(), prizeTakerId, knockedOutOwnerId, events))
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

    private GameState resolveKnockoutConsequencesWithoutActiveReplacement(GameState state, KnockoutResult knockout, PlayerId prizeTakerId, PlayerId knockedOutOwnerId, List<GameEvent> events) {
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
        return afterPrizes;
    }

    private GameState resolveSimultaneousActiveKnockouts(GameState state, PlayerId attackerId, PlayerId defenderId, List<GameEvent> events) {
        KnockoutResolver.KnockoutResolution defenderResolution = knockoutResolver.resolveActiveKnockout(state, defenderId, events)
                .orElseThrow(() -> new KnockoutException("Defender active Pokemon is not knocked out"));
        KnockoutResult defenderKnockout = defenderResolution.knockout();
        KnockoutResolver.KnockoutResolution attackerResolution = knockoutResolver.resolveActiveKnockout(defenderResolution.state(), attackerId, events)
                .orElseThrow(() -> new KnockoutException("Attacker active Pokemon is not knocked out"));
        KnockoutResult attackerKnockout = attackerResolution.knockout();

        PrizeResolver.PrizeResolution afterAttackerPrizes = prizeResolver.takePrizes(attackerResolution.state(), attackerId, defenderKnockout.prizeValue(), events);
        PrizeResolver.PrizeResolution afterDefenderPrizes = prizeResolver.takePrizes(afterAttackerPrizes.state(), defenderId, attackerKnockout.prizeValue(), events);
        GameState afterPrizes = afterDefenderPrizes.state();

        java.util.Optional<GameFinishResult> attackerWin = victoryConditionChecker.checkAfterKnockout(afterPrizes, attackerId, defenderId);
        java.util.Optional<GameFinishResult> defenderWin = victoryConditionChecker.checkAfterKnockout(afterPrizes, defenderId, attackerId);
        if (attackerWin.isPresent() && defenderWin.isPresent()) {
            GameFinishResult result = victoryConditionChecker.suddenDeathRequired(List.of(FinishReason.SIMULTANEOUS_WIN, FinishReason.SUDDEN_DEATH_REQUIRED));
            events.add(new SuddenDeathRequiredEvent(afterPrizes.getGameId(), result.reasons()));
            return new GameState(afterPrizes.getGameId(), GameStatus.FINISHED, afterPrizes.getPlayerOneState(), afterPrizes.getPlayerTwoState(), afterPrizes.getTurnState(), afterPrizes.getActiveStadium().orElse(null), result, null, events);
        }
        if (attackerWin.isPresent()) {
            return finished(afterPrizes, attackerWin.get(), attackerId, defenderId, events);
        }
        if (defenderWin.isPresent()) {
            return finished(afterPrizes, defenderWin.get(), defenderId, attackerId, events);
        }
        return afterPrizes;
    }

    private GameState finished(GameState state, GameFinishResult result, PlayerId winnerId, PlayerId loserId, List<GameEvent> events) {
        FinishReason primaryReason = result.reasons().get(0);
        events.add(new VictoryDetectedEvent(state.getGameId(), winnerId, loserId, primaryReason));
        events.add(new GameFinishedEvent(state.getGameId(), winnerId, loserId, primaryReason));
        return new GameState(state.getGameId(), GameStatus.FINISHED, state.getPlayerOneState(), state.getPlayerTwoState(), state.getTurnState(), state.getActiveStadium().orElse(null), result, null, events);
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

    private java.util.Optional<PokemonInPlay> activePokemon(GameState state, PlayerId playerId) {
        return playerState(state, playerId).getBoard().getActivePokemon().map(ActivePokemon::getPokemon);
    }
}
