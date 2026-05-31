package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.DeckSearchedEvent;
import com.tpi.pokemon.game.engine.event.DeckShuffledEvent;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.PendingSelectionRequiredEvent;
import com.tpi.pokemon.game.engine.random.DeckShuffler;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GreatBallSearchEffectHandler implements EffectHandler {
    private final DeckShuffler deckShuffler;

    public GreatBallSearchEffectHandler() {
        this(new RandomDeckShuffler());
    }

    public GreatBallSearchEffectHandler(DeckShuffler deckShuffler) {
        this.deckShuffler = Objects.requireNonNull(deckShuffler, "deckShuffler must not be null");
    }

    @Override public EffectType type() { return EffectType.GREAT_BALL_SEARCH; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        PlayerId playerId = context.actingPlayerId();
        PlayerGameState player = EffectStateSupport.playerState(context.state(), playerId);
        List<CardInstance> deck = player.getDeck().getCards();
        List<CardInstance> inspected = deck.stream().limit(definition.amount()).toList();
        List<CardInstanceId> candidates = inspected.stream()
                .filter(card -> definition.cardFilter().matches(card))
                .map(CardInstance::id)
                .toList();
        if (definition.selectedCardIds().isEmpty() && !candidates.isEmpty()) {
            PendingEffectSelection pending = new PendingEffectSelection(playerId, definition.type(), context.sourceId(), EffectCardZone.DECK, definition.target(), 0, 1, definition.cardFilter(), true, true, definition, candidates);
            context.events().add(new PendingSelectionRequiredEvent(context.state().getGameId(), playerId, definition.type(), context.sourceId(), EffectCardZone.DECK, definition.target(), 0, 1));
            return new EffectResult(context.state(), pending);
        }

        if (definition.selectedCardIds().size() > 1) {
            throw new EffectException("Great Ball can select at most one Pokemon");
        }
        List<CardInstance> selected = EffectCardMovementSupport.selectedFrom(inspected, definition.selectedCardIds(), "TOP_DECK_CARDS");
        EffectCardMovementSupport.requireMatchesFilter(selected, definition.cardFilter());
        List<CardInstance> remaining = new ArrayList<>(deck);
        remaining.removeAll(selected);
        DeckZone shuffledDeck = new DeckZone(deckShuffler.shuffle(remaining));
        HandZone hand = player.getHand().withCardsAdded(selected);
        PlayerGameState updatedPlayer = EffectStateSupport.withDeckAndHand(player, shuffledDeck, hand);
        context.events().add(new DeckSearchedEvent(context.state().getGameId(), playerId, definition.selectedCardIds(), definition.amount(), true, true));
        context.events().add(new DeckShuffledEvent(context.state().getGameId(), playerId, shuffledDeck.getCards().size()));
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), playerId, definition.type(), context.sourceId()));
        return new EffectResult(EffectStateSupport.withPlayer(context.state(), updatedPlayer));
    }
}
