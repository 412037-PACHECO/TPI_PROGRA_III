package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.Bench;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.CardsShuffledIntoDeckEvent;
import com.tpi.pokemon.game.engine.event.DeckShuffledEvent;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.PendingSelectionRequiredEvent;
import com.tpi.pokemon.game.engine.knockout.ActiveReplacementReason;
import com.tpi.pokemon.game.engine.knockout.PendingActiveReplacement;
import com.tpi.pokemon.game.engine.random.DeckShuffler;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ReturnPokemonToDeckEffectHandler implements EffectHandler {
    private final DeckShuffler deckShuffler;

    public ReturnPokemonToDeckEffectHandler() {
        this(new RandomDeckShuffler());
    }

    public ReturnPokemonToDeckEffectHandler(DeckShuffler deckShuffler) {
        this.deckShuffler = Objects.requireNonNull(deckShuffler, "deckShuffler must not be null");
    }

    @Override public EffectType type() { return EffectType.RETURN_POKEMON_TO_DECK; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        PlayerId playerId = context.actingPlayerId();
        PlayerGameState player = EffectStateSupport.playerState(context.state(), playerId);
        if (definition.targetBenchIndex() < -1) {
            List<com.tpi.pokemon.game.domain.value.CardInstanceId> candidates = new ArrayList<>();
            player.getBoard().getActivePokemon().ifPresent(active -> candidates.add(active.getPokemon().getTopCard().id()));
            player.getBoard().getBench().getPokemon().forEach(pokemon -> candidates.add(pokemon.getTopCard().id()));
            PendingEffectSelection pending = new PendingEffectSelection(playerId, definition.type(), context.sourceId(), EffectCardZone.IN_PLAY, definition.target(), 1, 1, definition.cardFilter(), false, true, definition, candidates);
            context.events().add(new PendingSelectionRequiredEvent(context.state().getGameId(), playerId, definition.type(), context.sourceId(), EffectCardZone.IN_PLAY, definition.target(), 1, 1));
            return new EffectResult(context.state(), pending);
        }
        PokemonInPlay target = EffectStateSupport.pokemonByBenchIndexOrActive(player, definition.targetBenchIndex());
        List<CardInstance> returned = new ArrayList<>(target.getEvolutionStack());
        returned.addAll(target.getAttachedCards().getCards());
        List<CardInstance> deckCards = new ArrayList<>(player.getDeck().getCards());
        deckCards.addAll(returned);
        DeckZone deck = new DeckZone(deckShuffler.shuffle(deckCards));
        BoardState board;
        PendingActiveReplacement pendingReplacement = context.state().getPendingActiveReplacement().orElse(null);
        if (definition.targetBenchIndex() < 0) {
            board = player.getBoard().withoutActivePokemon();
            if (!player.getBoard().getBench().isEmpty()) {
                pendingReplacement = new PendingActiveReplacement(playerId, ActiveReplacementReason.ACTIVE_RETURNED_TO_DECK);
            }
        } else {
            Bench bench = player.getBoard().getBench().withoutPokemonAt(definition.targetBenchIndex());
            board = player.getBoard().withBench(bench);
        }
        PlayerGameState updatedPlayer = new PlayerGameState(player.getPlayerId(), deck, player.getHand(), player.getPrizeCards(), player.getDiscardPile(), board, player.getTurnsTaken());
        context.events().add(new CardsShuffledIntoDeckEvent(context.state().getGameId(), playerId, returned.stream().map(CardInstance::id).toList(), deck.getCards().size()));
        context.events().add(new DeckShuffledEvent(context.state().getGameId(), playerId, deck.getCards().size()));
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), playerId, definition.type(), context.sourceId()));
        return new EffectResult(EffectStateSupport.withPlayerAndPendingActiveReplacement(context.state(), updatedPlayer, pendingReplacement));
    }
}
