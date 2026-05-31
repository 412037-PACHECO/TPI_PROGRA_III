package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.DeckShuffledEvent;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.PendingSelectionRequiredEvent;
import com.tpi.pokemon.game.engine.event.PokemonEvolvedEvent;
import com.tpi.pokemon.game.engine.random.DeckShuffler;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EvosodaEvolveEffectHandler implements EffectHandler {
    private final DeckShuffler deckShuffler;

    public EvosodaEvolveEffectHandler() {
        this(new RandomDeckShuffler());
    }

    public EvosodaEvolveEffectHandler(DeckShuffler deckShuffler) {
        this.deckShuffler = Objects.requireNonNull(deckShuffler, "deckShuffler must not be null");
    }

    @Override public EffectType type() { return EffectType.EVOSODA_EVOLVE; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        PlayerId playerId = context.actingPlayerId();
        PlayerGameState player = EffectStateSupport.playerState(context.state(), playerId);
        if (definition.selectedCardIds().isEmpty() || definition.targetBenchIndex() < -1) {
            List<CardInstanceId> candidates = player.getDeck().getCards().stream()
                    .filter(card -> canEvolveAny(card, player))
                    .map(CardInstance::id)
                    .toList();
            PendingEffectSelection pending = new PendingEffectSelection(playerId, definition.type(), context.sourceId(), EffectCardZone.DECK, definition.target(), 0, 1, definition.cardFilter(), false, true, definition, candidates);
            context.events().add(new PendingSelectionRequiredEvent(context.state().getGameId(), playerId, definition.type(), context.sourceId(), EffectCardZone.DECK, definition.target(), 0, 1));
            return new EffectResult(context.state(), pending);
        }
        if (definition.selectedCardIds().size() != 1) {
            throw new EffectException("Evosoda requires exactly one evolution card");
        }
        CardInstance evolution = EffectCardMovementSupport.selectedFrom(player.getDeck().getCards(), definition.selectedCardIds(), "DECK").get(0);
        PokemonInPlay target = EffectStateSupport.pokemonByBenchIndexOrActive(player, definition.targetBenchIndex());
        if (!evolution.definition().canEvolve() || !evolution.definition().evolvesFrom().equalsIgnoreCase(target.getTopCard().definition().name())) {
            throw new EffectException("Selected evolution does not match target Pokemon");
        }
        PokemonInPlay evolved = target.evolve(evolution, context.state().getTurnState().turnNumber());
        BoardState board = EffectStateSupport.withPokemonAtBenchIndexOrActive(player.getBoard(), evolved, definition.targetBenchIndex());
        List<CardInstance> remainingDeck = new ArrayList<>(EffectCardMovementSupport.withoutSelected(player.getDeck().getCards(), definition.selectedCardIds()));
        DeckZone deck = new DeckZone(deckShuffler.shuffle(remainingDeck));
        PlayerGameState updatedPlayer = new PlayerGameState(player.getPlayerId(), deck, player.getHand(), player.getPrizeCards(), player.getDiscardPile(), board, player.getTurnsTaken());
        context.events().add(new PokemonEvolvedEvent(context.state().getGameId(), playerId, evolution.id(), target.getTopCard().id()));
        context.events().add(new DeckShuffledEvent(context.state().getGameId(), playerId, deck.getCards().size()));
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), playerId, definition.type(), context.sourceId()));
        return new EffectResult(EffectStateSupport.withPlayer(context.state(), updatedPlayer));
    }

    private boolean canEvolveAny(CardInstance card, PlayerGameState player) {
        if (!card.definition().canEvolve()) return false;
        if (player.getBoard().getActivePokemon().map(active -> evolvesFrom(card, active.getPokemon())).orElse(false)) return true;
        return player.getBoard().getBench().getPokemon().stream().anyMatch(pokemon -> evolvesFrom(card, pokemon));
    }

    private boolean evolvesFrom(CardInstance evolution, PokemonInPlay target) {
        return evolution.definition().evolvesFrom().equalsIgnoreCase(target.getTopCard().definition().name());
    }
}
