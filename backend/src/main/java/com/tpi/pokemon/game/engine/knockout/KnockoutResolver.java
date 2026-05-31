package com.tpi.pokemon.game.engine.knockout;

import com.tpi.pokemon.game.domain.model.ActivePokemon;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.CardsDiscardedEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.PokemonKnockedOutEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class KnockoutResolver {
    public boolean isKnockedOut(PokemonInPlay pokemon) {
        Integer hp = pokemon.getTopCard().definition().hp();
        if (hp == null) {
            return false;
        }
        return pokemon.getDamageCounters() * 10 >= hp;
    }

    public Optional<KnockoutResolution> resolveActiveKnockout(GameState state, PlayerId ownerId, List<GameEvent> events) {
        PlayerGameState owner = playerState(state, ownerId);
        Optional<PokemonInPlay> maybeActive = owner.getBoard().getActivePokemon().map(ActivePokemon::getPokemon);
        if (maybeActive.isEmpty() || !isKnockedOut(maybeActive.get())) {
            return Optional.empty();
        }

        PokemonInPlay knockedOut = maybeActive.get();
        List<CardInstance> discardedCards = new ArrayList<>(knockedOut.getEvolutionStack());
        discardedCards.addAll(knockedOut.getAttachedCards().getCards());

        BoardState boardWithoutActive = owner.getBoard().withoutActivePokemon();
        PlayerGameState updatedOwner = new PlayerGameState(
                owner.getPlayerId(),
                owner.getDeck(),
                owner.getHand(),
                owner.getPrizeCards(),
                owner.getDiscardPile().withCardsAdded(discardedCards),
                boardWithoutActive,
                owner.getTurnsTaken()
        );
        GameState updatedState = withPlayer(state, updatedOwner, events);

        Integer hp = knockedOut.getTopCard().definition().hp();
        events.add(new PokemonKnockedOutEvent(state.getGameId(), ownerId, knockedOut.getTopCard().id(), knockedOut.getDamageCounters(), hp == null ? 0 : hp));
        events.add(new CardsDiscardedEvent(state.getGameId(), ownerId, discardedCards.stream().map(CardInstance::id).toList(), "KNOCKOUT"));

        return Optional.of(new KnockoutResolution(updatedState, new KnockoutResult(ownerId, knockedOut, discardedCards, knockedOut.getTopCard().definition().prizeValue())));
    }

    public Optional<KnockoutResolution> resolveBenchKnockout(GameState state, PlayerId ownerId, int benchIndex, List<GameEvent> events) {
        PlayerGameState owner = playerState(state, ownerId);
        List<PokemonInPlay> bench = owner.getBoard().getBench().getPokemon();
        if (benchIndex < 0 || benchIndex >= bench.size()) {
            throw new KnockoutException("Bench index is invalid");
        }
        PokemonInPlay knockedOut = bench.get(benchIndex);
        if (!isKnockedOut(knockedOut)) {
            return Optional.empty();
        }

        List<CardInstance> discardedCards = new ArrayList<>(knockedOut.getEvolutionStack());
        discardedCards.addAll(knockedOut.getAttachedCards().getCards());
        List<PokemonInPlay> updatedBench = new ArrayList<>(bench);
        updatedBench.remove(benchIndex);
        BoardState boardWithoutBenchPokemon = new BoardState(owner.getBoard().getActivePokemon().orElse(null), new com.tpi.pokemon.game.domain.model.Bench(updatedBench));
        PlayerGameState updatedOwner = new PlayerGameState(
                owner.getPlayerId(),
                owner.getDeck(),
                owner.getHand(),
                owner.getPrizeCards(),
                owner.getDiscardPile().withCardsAdded(discardedCards),
                boardWithoutBenchPokemon,
                owner.getTurnsTaken()
        );
        GameState updatedState = withPlayer(state, updatedOwner, events);
        Integer hp = knockedOut.getTopCard().definition().hp();
        events.add(new PokemonKnockedOutEvent(state.getGameId(), ownerId, knockedOut.getTopCard().id(), knockedOut.getDamageCounters(), hp == null ? 0 : hp));
        events.add(new CardsDiscardedEvent(state.getGameId(), ownerId, discardedCards.stream().map(CardInstance::id).toList(), "KNOCKOUT"));
        return Optional.of(new KnockoutResolution(updatedState, new KnockoutResult(ownerId, knockedOut, discardedCards, knockedOut.getTopCard().definition().prizeValue())));
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

    public record KnockoutResolution(GameState state, KnockoutResult knockout) {}
}
