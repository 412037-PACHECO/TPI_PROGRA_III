package com.tpi.pokemon.game.engine.special;

import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.domain.model.ActivePokemon;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.BetweenTurnsResolvedEvent;
import com.tpi.pokemon.game.engine.event.BurnCheckResolvedEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.GameFinishedEvent;
import com.tpi.pokemon.game.engine.event.ParalysisClearedEvent;
import com.tpi.pokemon.game.engine.event.SleepCheckResolvedEvent;
import com.tpi.pokemon.game.engine.event.SpecialConditionDamageAppliedEvent;
import com.tpi.pokemon.game.engine.event.SpecialConditionRemovedEvent;
import com.tpi.pokemon.game.engine.event.SuddenDeathRequiredEvent;
import com.tpi.pokemon.game.engine.event.VictoryDetectedEvent;
import com.tpi.pokemon.game.engine.event.ActivePokemonReplacementRequiredEvent;
import com.tpi.pokemon.game.engine.knockout.ActiveReplacementReason;
import com.tpi.pokemon.game.engine.knockout.KnockoutResolver;
import com.tpi.pokemon.game.engine.knockout.PendingActiveReplacement;
import com.tpi.pokemon.game.engine.knockout.PrizeResolver;
import com.tpi.pokemon.game.engine.random.CoinFlipProvider;
import com.tpi.pokemon.game.engine.random.CoinFlipResult;
import com.tpi.pokemon.game.engine.random.RandomCoinFlipProvider;
import com.tpi.pokemon.game.engine.victory.FinishReason;
import com.tpi.pokemon.game.engine.victory.GameFinishResult;
import com.tpi.pokemon.game.engine.victory.VictoryConditionChecker;
import java.util.List;
import java.util.Objects;

public final class BetweenTurnsService {
    private final CoinFlipProvider coinFlipProvider;
    private final KnockoutResolver knockoutResolver;
    private final PrizeResolver prizeResolver;
    private final VictoryConditionChecker victoryConditionChecker;

    public BetweenTurnsService() {
        this(new RandomCoinFlipProvider());
    }

    public BetweenTurnsService(CoinFlipProvider coinFlipProvider) {
        this(coinFlipProvider, new KnockoutResolver(), new PrizeResolver(), new VictoryConditionChecker());
    }

    public BetweenTurnsService(CoinFlipProvider coinFlipProvider, KnockoutResolver knockoutResolver, PrizeResolver prizeResolver, VictoryConditionChecker victoryConditionChecker) {
        this.coinFlipProvider = Objects.requireNonNull(coinFlipProvider, "coinFlipProvider must not be null");
        this.knockoutResolver = Objects.requireNonNull(knockoutResolver, "knockoutResolver must not be null");
        this.prizeResolver = Objects.requireNonNull(prizeResolver, "prizeResolver must not be null");
        this.victoryConditionChecker = Objects.requireNonNull(victoryConditionChecker, "victoryConditionChecker must not be null");
    }

    public GameState resolveBetweenTurns(GameState state, List<GameEvent> events) {
        GameState resolved = resolveConditionsForActive(state, state.getPlayerOneState().getPlayerId(), events);
        if (resolved.getStatus() == GameStatus.ACTIVE) {
            resolved = resolveConditionsForActive(resolved, resolved.getPlayerTwoState().getPlayerId(), events);
        }
        if (bothActivePokemonAreKnockedOut(resolved)) {
            GameFinishResult result = victoryConditionChecker.suddenDeathRequired(List.of(FinishReason.SIMULTANEOUS_WIN, FinishReason.SUDDEN_DEATH_REQUIRED));
            events.add(new SuddenDeathRequiredEvent(resolved.getGameId(), result.reasons()));
            events.add(new BetweenTurnsResolvedEvent(resolved.getGameId(), resolved.getTurnState().turnNumber()));
            return new GameState(resolved.getGameId(), GameStatus.FINISHED, resolved.getPlayerOneState(), resolved.getPlayerTwoState(), resolved.getTurnState(), resolved.getActiveStadium().orElse(null), result, null, events);
        }
        if (resolved.getStatus() == GameStatus.ACTIVE) {
            resolved = resolveKnockoutForOwner(resolved, resolved.getPlayerOneState().getPlayerId(), resolved.getPlayerTwoState().getPlayerId(), events);
        }
        if (resolved.getStatus() == GameStatus.ACTIVE && resolved.getPendingActiveReplacement().isEmpty()) {
            resolved = resolveKnockoutForOwner(resolved, resolved.getPlayerTwoState().getPlayerId(), resolved.getPlayerOneState().getPlayerId(), events);
        }
        events.add(new BetweenTurnsResolvedEvent(resolved.getGameId(), resolved.getTurnState().turnNumber()));
        return new GameState(resolved.getGameId(), resolved.getStatus(), resolved.getPlayerOneState(), resolved.getPlayerTwoState(), resolved.getTurnState(), resolved.getActiveStadium().orElse(null), resolved.getFinishResult().orElse(null), resolved.getPendingActiveReplacement().orElse(null), events);
    }

    private boolean bothActivePokemonAreKnockedOut(GameState state) {
        return state.getPlayerOneState().getBoard().getActivePokemon().map(ActivePokemon::getPokemon).map(knockoutResolver::isKnockedOut).orElse(false)
                && state.getPlayerTwoState().getBoard().getActivePokemon().map(ActivePokemon::getPokemon).map(knockoutResolver::isKnockedOut).orElse(false);
    }

    private GameState resolveConditionsForActive(GameState state, PlayerId ownerId, List<GameEvent> events) {
        PlayerGameState owner = playerState(state, ownerId);
        if (owner.getBoard().getActivePokemon().isEmpty()) {
            return state;
        }
        PokemonInPlay active = owner.getBoard().getActivePokemon().orElseThrow().getPokemon();
        PokemonInPlay updated = active;

        if (updated.hasSpecialCondition(SpecialCondition.POISONED)) {
            updated = updated.applyDamage(10);
            events.add(new SpecialConditionDamageAppliedEvent(state.getGameId(), ownerId, updated.getTopCard().id(), SpecialCondition.POISONED, 10, updated.getDamageCounters()));
        }
        if (updated.hasSpecialCondition(SpecialCondition.BURNED)) {
            CoinFlipResult burnResult = coinFlipProvider.flip();
            events.add(new BurnCheckResolvedEvent(state.getGameId(), ownerId, updated.getTopCard().id(), burnResult));
            if (burnResult == CoinFlipResult.TAILS) {
                updated = updated.applyDamage(20);
                events.add(new SpecialConditionDamageAppliedEvent(state.getGameId(), ownerId, updated.getTopCard().id(), SpecialCondition.BURNED, 20, updated.getDamageCounters()));
            }
        }
        if (updated.hasSpecialCondition(SpecialCondition.ASLEEP)) {
            CoinFlipResult sleepResult = coinFlipProvider.flip();
            events.add(new SleepCheckResolvedEvent(state.getGameId(), ownerId, updated.getTopCard().id(), sleepResult));
            if (sleepResult == CoinFlipResult.HEADS) {
                updated = updated.removeSpecialCondition(SpecialCondition.ASLEEP);
                events.add(new SpecialConditionRemovedEvent(state.getGameId(), ownerId, updated.getTopCard().id(), SpecialCondition.ASLEEP, "ASLEEP_RECOVERY"));
            }
        }
        if (updated.hasSpecialCondition(SpecialCondition.PARALYZED)) {
            updated = updated.removeSpecialCondition(SpecialCondition.PARALYZED);
            events.add(new ParalysisClearedEvent(state.getGameId(), ownerId, updated.getTopCard().id()));
            events.add(new SpecialConditionRemovedEvent(state.getGameId(), ownerId, updated.getTopCard().id(), SpecialCondition.PARALYZED, "PARALYSIS_RECOVERY"));
        }
        return withActivePokemon(state, ownerId, updated, events);
    }

    private GameState resolveKnockoutForOwner(GameState state, PlayerId ownerId, PlayerId prizeTakerId, List<GameEvent> events) {
        return knockoutResolver.resolveActiveKnockout(state, ownerId, events)
                .map(resolution -> resolveKnockoutConsequences(resolution.state(), ownerId, prizeTakerId, resolution.knockout().prizeValue(), events))
                .orElse(state);
    }

    private GameState resolveKnockoutConsequences(GameState state, PlayerId knockedOutOwnerId, PlayerId prizeTakerId, int prizeValue, List<GameEvent> events) {
        PrizeResolver.PrizeResolution prizeResolution = prizeResolver.takePrizes(state, prizeTakerId, prizeValue, events);
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

    private GameState withActivePokemon(GameState state, PlayerId ownerId, PokemonInPlay active, List<GameEvent> events) {
        PlayerGameState owner = playerState(state, ownerId);
        BoardState board = new BoardState(new ActivePokemon(active), owner.getBoard().getBench(), owner.getBoard().getActiveStadium().orElse(null));
        PlayerGameState updatedOwner = owner.withBoard(board);
        PlayerGameState playerOne = state.getPlayerOneState().getPlayerId().equals(ownerId) ? updatedOwner : state.getPlayerOneState();
        PlayerGameState playerTwo = state.getPlayerTwoState().getPlayerId().equals(ownerId) ? updatedOwner : state.getPlayerTwoState();
        return new GameState(state.getGameId(), state.getStatus(), playerOne, playerTwo, state.getTurnState(), state.getActiveStadium().orElse(null), state.getFinishResult().orElse(null), state.getPendingActiveReplacement().orElse(null), events);
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
