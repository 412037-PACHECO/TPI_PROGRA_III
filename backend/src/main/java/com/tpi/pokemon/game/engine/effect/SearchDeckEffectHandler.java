package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.DeckSearchedEvent;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.PendingSelectionRequiredEvent;
import java.util.List;

public final class SearchDeckEffectHandler implements EffectHandler {
    @Override public EffectType type() { return EffectType.SEARCH_DECK; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        PlayerId ownerId = EffectStateSupport.ownerFor(context, definition.target());
        if (definition.selectedCardIds().isEmpty()) {
            PendingEffectSelection pending = new PendingEffectSelection(ownerId, definition.type(), context.sourceId(), EffectCardZone.DECK, definition.target(), 0, definition.amount(), definition.cardFilter());
            context.events().add(new PendingSelectionRequiredEvent(context.state().getGameId(), ownerId, definition.type(), context.sourceId(), EffectCardZone.DECK, definition.target(), 0, definition.amount()));
            return new EffectResult(context.state(), pending);
        }
        if (definition.selectedCardIds().size() > definition.amount()) {
            throw new EffectException("Selected more cards than allowed by search effect");
        }
        PlayerGameState owner = EffectStateSupport.playerState(context.state(), ownerId);
        List<CardInstance> selected = EffectCardMovementSupport.selectedFrom(owner.getDeck().getCards(), definition.selectedCardIds(), "DECK");
        EffectCardMovementSupport.requireMatchesFilter(selected, definition.cardFilter());
        DeckZone deck = new DeckZone(EffectCardMovementSupport.withoutSelected(owner.getDeck().getCards(), definition.selectedCardIds()));
        HandZone hand = owner.getHand().withCardsAdded(selected);
        PlayerGameState updatedOwner = EffectStateSupport.withDeckAndHand(owner, deck, hand);
        context.events().add(new DeckSearchedEvent(context.state().getGameId(), ownerId, definition.selectedCardIds(), definition.amount(), definition.revealSelectedCards(), definition.requiresShuffle()));
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), context.actingPlayerId(), definition.type(), context.sourceId()));
        return new EffectResult(EffectStateSupport.withPlayer(context.state(), updatedOwner));
    }
}
