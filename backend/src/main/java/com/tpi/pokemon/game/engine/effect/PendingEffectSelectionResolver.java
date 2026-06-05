package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.random.CoinFlipProvider;
import com.tpi.pokemon.game.engine.random.CoinFlipResult;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public final class PendingEffectSelectionResolver {
    private final EffectExecutionService executionService;
    private final CoinFlipProvider coinFlipProvider;

    public PendingEffectSelectionResolver() {
        this(new EffectExecutionService(), () -> CoinFlipResult.HEADS);
    }

    public PendingEffectSelectionResolver(EffectExecutionService executionService, CoinFlipProvider coinFlipProvider) {
        this.executionService = Objects.requireNonNull(executionService, "executionService must not be null");
        this.coinFlipProvider = Objects.requireNonNull(coinFlipProvider, "coinFlipProvider must not be null");
    }

    public EffectResult resolve(GameState state, PendingEffectSelection pending, ResolvePendingEffectSelectionCommand command) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(pending, "pending must not be null");
        Objects.requireNonNull(command, "command must not be null");
        if (!pending.playerId().equals(command.playerId())) {
            throw new EffectException("Only the pending selection player can resolve this selection");
        }
        List<CardInstanceId> selected = command.selectedCardIds();
        if (new LinkedHashSet<>(selected).size() != selected.size()) {
            throw new EffectException("Selected cards must not contain duplicates");
        }
        if (selected.size() < pending.minSelections() || selected.size() > pending.maxSelections()) {
            throw new EffectException("Selected card count is outside allowed bounds");
        }
        if (!pending.candidateCardIds().isEmpty() && !new LinkedHashSet<>(pending.candidateCardIds()).containsAll(selected)) {
            throw new EffectException("Selected cards must be part of the pending candidates");
        }
        EffectDefinition continuation = pending.continuationEffect();
        if (continuation == null) {
            throw new EffectException("Pending selection does not have a continuation effect");
        }

        int targetBenchIndex = resolveTargetBenchIndex(state, pending, command, continuation, selected);
        if (selected.isEmpty() && pending.minSelections() == 0) {
            return resolveEmptyOptionalSelection(state, pending, command, continuation);
        }

        EffectDefinition resolvedDefinition = withSelection(continuation, selected, targetBenchIndex);
        List<GameEvent> events = new ArrayList<>(state.getEvents());
        EffectResult result = executionService.execute(resolvedDefinition, context(state, pending, command, events));
        result = new EffectResult(withEvents(result.state(), events), result.pendingSelection());
        if (result.pendingSelectionOptional().isPresent()) {
            return result;
        }
        return shuffleAfterSearchDeckIfNeeded(result.state(), pending, command);
    }

    private EffectResult resolveEmptyOptionalSelection(GameState state, PendingEffectSelection pending, ResolvePendingEffectSelectionCommand command, EffectDefinition continuation) {
        if (pending.requiresShuffle() && continuation.sourceZone() == EffectCardZone.DECK) {
            EffectDefinition shuffle = EffectDefinition.shuffleDeck(continuation.target(), continuation.timing());
            List<GameEvent> events = new ArrayList<>(state.getEvents());
            EffectResult result = executionService.execute(shuffle, context(state, pending, command, events));
            return new EffectResult(withEvents(result.state(), events), result.pendingSelection());
        }
        return new EffectResult(state);
    }

    private EffectResult shuffleAfterSearchDeckIfNeeded(GameState state, PendingEffectSelection pending, ResolvePendingEffectSelectionCommand command) {
        if (pending.effectType() != EffectType.SEARCH_DECK || !pending.requiresShuffle()) {
            return new EffectResult(state);
        }
        EffectDefinition shuffle = EffectDefinition.shuffleDeck(pending.target(), pending.continuationEffect().timing());
        List<GameEvent> events = new ArrayList<>(state.getEvents());
        EffectResult result = executionService.execute(shuffle, context(state, pending, command, events));
        return new EffectResult(withEvents(result.state(), events), result.pendingSelection());
    }

    private EffectExecutionContext context(GameState state, PendingEffectSelection pending, ResolvePendingEffectSelectionCommand command, List<GameEvent> events) {
        return new EffectExecutionContext(state, command.playerId(), opponent(state, command.playerId()), pending.sourceId(), events, coinFlipProvider);
    }

    private GameState withEvents(GameState state, List<GameEvent> events) {
        return new GameState(state.getGameId(), state.getStatus(), state.getPlayerOneState(), state.getPlayerTwoState(), state.getTurnState(), state.getActiveStadium().orElse(null), state.getFinishResult().orElse(null), state.getPendingActiveReplacement().orElse(null), events);
    }

    private int resolveTargetBenchIndex(GameState state, PendingEffectSelection pending, ResolvePendingEffectSelectionCommand command, EffectDefinition continuation, List<CardInstanceId> selected) {
        if (command.targetBenchIndex() != null) {
            return command.targetBenchIndex();
        }
        if (continuation.targetBenchIndex() >= -1) {
            return continuation.targetBenchIndex();
        }
        if (pending.sourceZone() == EffectCardZone.IN_PLAY && selected.size() == 1) {
            return benchIndexForTopCard(EffectStateSupport.playerState(state, command.playerId()), selected.get(0));
        }
        if (pending.effectType() == EffectType.EVOSODA_EVOLVE && selected.size() == 1) {
            return unambiguousEvolutionTarget(EffectStateSupport.playerState(state, command.playerId()), selected.get(0));
        }
        return continuation.targetBenchIndex();
    }

    private int benchIndexForTopCard(PlayerGameState player, CardInstanceId selectedTopCardId) {
        if (player.getBoard().getActivePokemon().map(active -> active.getPokemon().getTopCard().id().equals(selectedTopCardId)).orElse(false)) {
            return -1;
        }
        List<PokemonInPlay> bench = player.getBoard().getBench().getPokemon();
        for (int i = 0; i < bench.size(); i++) {
            if (bench.get(i).getTopCard().id().equals(selectedTopCardId)) {
                return i;
            }
        }
        throw new EffectException("Selected in-play Pokemon is not valid for this pending selection");
    }

    private int unambiguousEvolutionTarget(PlayerGameState player, CardInstanceId selectedEvolutionId) {
        CardInstance evolution = player.getDeck().getCards().stream()
                .filter(card -> card.id().equals(selectedEvolutionId))
                .findFirst()
                .orElseThrow(() -> new EffectException("Selected evolution is not in deck"));
        List<Integer> targets = new ArrayList<>();
        player.getBoard().getActivePokemon()
                .filter(active -> evolvesFrom(evolution, active.getPokemon()))
                .ifPresent(active -> targets.add(-1));
        List<PokemonInPlay> bench = player.getBoard().getBench().getPokemon();
        for (int i = 0; i < bench.size(); i++) {
            if (evolvesFrom(evolution, bench.get(i))) {
                targets.add(i);
            }
        }
        if (targets.size() != 1) {
            throw new EffectException("Evosoda target is ambiguous and must be provided");
        }
        return targets.get(0);
    }

    private boolean evolvesFrom(CardInstance evolution, PokemonInPlay target) {
        return evolution.definition().canEvolve() && evolution.definition().evolvesFrom().equalsIgnoreCase(target.getTopCard().definition().name());
    }

    private EffectDefinition withSelection(EffectDefinition definition, List<CardInstanceId> selected, int targetBenchIndex) {
        return new EffectDefinition(
                definition.type(),
                definition.timing(),
                definition.target(),
                definition.amount(),
                definition.condition(),
                definition.children(),
                definition.headsEffect(),
                definition.tailsEffect(),
                selected,
                definition.sourceZone(),
                definition.destinationZone(),
                definition.cardFilter(),
                targetBenchIndex,
                definition.sourceBenchIndex(),
                definition.destinationBenchIndex(),
                definition.revealSelectedCards(),
                definition.requiresShuffle()
        );
    }

    private com.tpi.pokemon.game.domain.value.PlayerId opponent(GameState state, com.tpi.pokemon.game.domain.value.PlayerId playerId) {
        if (state.getPlayerOneState().getPlayerId().equals(playerId)) return state.getPlayerTwoState().getPlayerId();
        if (state.getPlayerTwoState().getPlayerId().equals(playerId)) return state.getPlayerOneState().getPlayerId();
        throw new EffectException("Player is not part of this game");
    }
}
