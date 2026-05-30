package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.DiscardPile;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.CardsDiscardedEvent;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.PendingSelectionRequiredEvent;
import java.util.List;

public final class DiscardCardsEffectHandler implements EffectHandler {
    @Override public EffectType type() { return EffectType.DISCARD_CARDS; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        PlayerId ownerId = EffectStateSupport.ownerFor(context, definition.target());
        if (definition.selectedCardIds().isEmpty()) {
            PendingEffectSelection pending = new PendingEffectSelection(ownerId, definition.type(), context.sourceId(), definition.sourceZone(), definition.target(), definition.amount(), definition.amount(), definition.cardFilter());
            context.events().add(new PendingSelectionRequiredEvent(context.state().getGameId(), ownerId, definition.type(), context.sourceId(), definition.sourceZone(), definition.target(), definition.amount(), definition.amount()));
            return new EffectResult(context.state(), pending);
        }
        PlayerGameState owner = EffectStateSupport.playerState(context.state(), ownerId);
        List<CardInstance> source = sourceCards(owner, definition.sourceZone());
        List<CardInstance> selected = EffectCardMovementSupport.selectedFrom(source, definition.selectedCardIds(), definition.sourceZone().name());
        if (selected.size() != definition.amount()) {
            throw new EffectException("Discard effect requires exactly " + definition.amount() + " selected cards");
        }
        EffectCardMovementSupport.requireMatchesFilter(selected, definition.cardFilter());
        DeckZone deck = owner.getDeck();
        HandZone hand = owner.getHand();
        if (definition.sourceZone() == EffectCardZone.HAND) {
            hand = new HandZone(EffectCardMovementSupport.withoutSelected(owner.getHand().getCards(), definition.selectedCardIds()));
        } else if (definition.sourceZone() == EffectCardZone.DECK) {
            deck = new DeckZone(EffectCardMovementSupport.withoutSelected(owner.getDeck().getCards(), definition.selectedCardIds()));
        } else {
            throw new EffectException("DiscardCardsEffectHandler supports HAND and DECK source zones");
        }
        DiscardPile discard = owner.getDiscardPile().withCardsAdded(selected);
        PlayerGameState updatedOwner = EffectStateSupport.withDeckHandAndDiscard(owner, deck, hand, discard);
        context.events().add(new CardsDiscardedEvent(context.state().getGameId(), ownerId, definition.selectedCardIds(), "EFFECT:" + context.sourceId()));
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), context.actingPlayerId(), definition.type(), context.sourceId()));
        return new EffectResult(EffectStateSupport.withPlayer(context.state(), updatedOwner));
    }

    private List<CardInstance> sourceCards(PlayerGameState owner, EffectCardZone sourceZone) {
        if (sourceZone == EffectCardZone.HAND) return owner.getHand().getCards();
        if (sourceZone == EffectCardZone.DECK) return owner.getDeck().getCards();
        throw new EffectException("Unsupported discard source zone " + sourceZone);
    }
}
