package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.DiscardPile;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.CardPlacedOnTopOfDeckEvent;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.PendingSelectionRequiredEvent;
import java.util.ArrayList;
import java.util.List;

public final class PutDiscardPokemonOnTopDeckEffectHandler implements EffectHandler {
    @Override public EffectType type() { return EffectType.PUT_DISCARD_POKEMON_ON_TOP_DECK; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        PlayerId playerId = EffectStateSupport.ownerFor(context, definition.target());
        if (definition.selectedCardIds().isEmpty()) {
            PendingEffectSelection pending = new PendingEffectSelection(playerId, definition.type(), context.sourceId(), EffectCardZone.DISCARD, definition.target(), 1, 1, definition.cardFilter(), false, false, definition);
            context.events().add(new PendingSelectionRequiredEvent(context.state().getGameId(), playerId, definition.type(), context.sourceId(), EffectCardZone.DISCARD, definition.target(), 1, 1));
            return new EffectResult(context.state(), pending);
        }
        if (definition.selectedCardIds().size() != 1) {
            throw new EffectException("Exactly one Pokémon must be selected from discard");
        }
        PlayerGameState player = EffectStateSupport.playerState(context.state(), playerId);
        List<CardInstance> selected = EffectCardMovementSupport.selectedFrom(player.getDiscardPile().getCards(), definition.selectedCardIds(), "DISCARD");
        EffectCardMovementSupport.requireMatchesFilter(selected, definition.cardFilter());
        CardInstance card = selected.get(0);
        List<CardInstance> deck = new ArrayList<>();
        deck.add(card);
        deck.addAll(player.getDeck().getCards());
        DiscardPile discard = new DiscardPile(EffectCardMovementSupport.withoutSelected(player.getDiscardPile().getCards(), definition.selectedCardIds()));
        PlayerGameState updated = EffectStateSupport.withDeckHandAndDiscard(player, new DeckZone(deck), player.getHand(), discard);
        context.events().add(new CardPlacedOnTopOfDeckEvent(context.state().getGameId(), playerId, card.id(), EffectCardZone.DISCARD.name()));
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), context.actingPlayerId(), definition.type(), context.sourceId()));
        return new EffectResult(EffectStateSupport.withPlayer(context.state(), updated));
    }
}
