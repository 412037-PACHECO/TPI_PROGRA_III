package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.DiscardPile;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.DamageCountersPlacedEvent;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.EnergyAttachedEvent;
import com.tpi.pokemon.game.engine.event.PendingSelectionRequiredEvent;
import java.util.List;

public final class AttachEnergyEffectHandler implements EffectHandler {
    @Override public EffectType type() { return EffectType.ATTACH_ENERGY; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        PlayerId ownerId = EffectStateSupport.ownerFor(context, definition.target());
        if (definition.selectedCardIds().isEmpty()) {
            PendingEffectSelection pending = new PendingEffectSelection(ownerId, definition.type(), context.sourceId(), definition.sourceZone(), definition.target(), 1, definition.amount(), definition.cardFilter());
            context.events().add(new PendingSelectionRequiredEvent(context.state().getGameId(), ownerId, definition.type(), context.sourceId(), definition.sourceZone(), definition.target(), 1, definition.amount()));
            return new EffectResult(context.state(), pending);
        }
        PlayerGameState owner = EffectStateSupport.playerState(context.state(), ownerId);
        List<CardInstance> source = sourceCards(owner, definition.sourceZone());
        List<CardInstance> selected = EffectCardMovementSupport.selectedFrom(source, definition.selectedCardIds(), definition.sourceZone().name());
        if (selected.stream().anyMatch(card -> card.definition().supertype() != CardSupertype.ENERGY)) {
            throw new EffectException("AttachEnergyEffectHandler can only attach Energy cards");
        }
        PokemonInPlay target = EffectStateSupport.pokemonByBenchIndexOrActive(owner, definition.targetBenchIndex());
        PokemonInPlay updatedPokemon = target;
        for (CardInstance energy : selected) {
            updatedPokemon = updatedPokemon.withAttachedEnergy(energy);
            if (definition.sourceZone() == EffectCardZone.HAND && energy.definition().energyProfile().attachDamageCountersFromHand() > 0) {
                int counters = energy.definition().energyProfile().attachDamageCountersFromHand();
                updatedPokemon = updatedPokemon.withDamageCounters(updatedPokemon.getDamageCounters() + counters);
                context.events().add(new DamageCountersPlacedEvent(context.state().getGameId(), ownerId, target.getTopCard().id(), counters, updatedPokemon.getDamageCounters(), energy.definition().cardId()));
            }
        }
        DeckZone deck = owner.getDeck();
        HandZone hand = owner.getHand();
        DiscardPile discard = owner.getDiscardPile();
        if (definition.sourceZone() == EffectCardZone.HAND) {
            hand = new HandZone(EffectCardMovementSupport.withoutSelected(owner.getHand().getCards(), definition.selectedCardIds()));
        } else if (definition.sourceZone() == EffectCardZone.DISCARD) {
            discard = new DiscardPile(EffectCardMovementSupport.withoutSelected(owner.getDiscardPile().getCards(), definition.selectedCardIds()));
        } else if (definition.sourceZone() == EffectCardZone.DECK) {
            deck = new DeckZone(EffectCardMovementSupport.withoutSelected(owner.getDeck().getCards(), definition.selectedCardIds()));
        } else {
            throw new EffectException("AttachEnergyEffectHandler supports HAND, DISCARD and DECK source zones");
        }
        BoardState board = EffectStateSupport.withPokemonAtBenchIndexOrActive(owner.getBoard(), updatedPokemon, definition.targetBenchIndex());
        PlayerGameState updatedOwner = EffectStateSupport.withDeckHandDiscardAndBoard(owner, deck, hand, discard, board);
        for (CardInstance energy : selected) {
            context.events().add(new EnergyAttachedEvent(context.state().getGameId(), ownerId, energy.id(), target.getTopCard().id()));
        }
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), context.actingPlayerId(), definition.type(), context.sourceId()));
        return new EffectResult(EffectStateSupport.withPlayer(context.state(), updatedOwner));
    }

    private List<CardInstance> sourceCards(PlayerGameState owner, EffectCardZone sourceZone) {
        if (sourceZone == EffectCardZone.HAND) return owner.getHand().getCards();
        if (sourceZone == EffectCardZone.DISCARD) return owner.getDiscardPile().getCards();
        if (sourceZone == EffectCardZone.DECK) return owner.getDeck().getCards();
        throw new EffectException("Unsupported attach source zone " + sourceZone);
    }
}
