package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.ActivePokemon;
import com.tpi.pokemon.game.domain.model.AttachedCards;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DiscardPile;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.EnergyDiscardedEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DiscardAttachedEnergyEffectHandler implements EffectHandler {
    @Override public EffectType type() { return EffectType.DISCARD_ATTACHED_ENERGY; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        PlayerId ownerId = EffectStateSupport.ownerFor(context, definition.target());
        PlayerGameState owner = EffectStateSupport.playerState(context.state(), ownerId);
        PokemonInPlay target = EffectStateSupport.activePokemon(context.state(), ownerId);
        List<CardInstance> energies = target.getAttachedCards().getEnergies();
        Set<CardInstanceId> selected = new LinkedHashSet<>(definition.selectedCardIds());
        if (selected.isEmpty()) {
            energies.stream().limit(definition.amount()).map(CardInstance::id).forEach(selected::add);
        }
        List<CardInstance> discarded = energies.stream().filter(card -> selected.contains(card.id())).toList();
        PokemonInPlay updatedPokemon = target.withoutAttachedEnergies(selected);
        List<CardInstance> discard = new ArrayList<>(owner.getDiscardPile().getCards());
        discard.addAll(discarded);
        BoardState board = new BoardState(new ActivePokemon(updatedPokemon), owner.getBoard().getBench(), owner.getBoard().getActiveStadium().orElse(null));
        PlayerGameState updatedOwner = EffectStateSupport.withDiscardAndBoard(owner, new DiscardPile(discard), board);
        context.events().add(new EnergyDiscardedEvent(context.state().getGameId(), ownerId, target.getTopCard().id(), discarded.stream().map(CardInstance::id).toList()));
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), context.actingPlayerId(), definition.type(), context.sourceId()));
        return new EffectResult(EffectStateSupport.withPlayer(context.state(), updatedOwner));
    }
}
