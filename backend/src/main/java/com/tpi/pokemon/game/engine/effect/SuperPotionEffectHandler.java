package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DiscardPile;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.EnergyDiscardedEvent;
import com.tpi.pokemon.game.engine.event.HealEffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.PendingSelectionRequiredEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SuperPotionEffectHandler implements EffectHandler {
    @Override public EffectType type() { return EffectType.SUPER_POTION; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        PlayerId playerId = context.actingPlayerId();
        PlayerGameState player = EffectStateSupport.playerState(context.state(), playerId);
        if (definition.targetBenchIndex() < -1) {
            List<CardInstanceId> candidates = new ArrayList<>();
            player.getBoard().getActivePokemon()
                    .filter(active -> canApplySuperPotion(active.getPokemon()))
                    .ifPresent(active -> candidates.add(active.getPokemon().getTopCard().id()));
            player.getBoard().getBench().getPokemon().stream()
                    .filter(this::canApplySuperPotion)
                    .forEach(pokemon -> candidates.add(pokemon.getTopCard().id()));
            PendingEffectSelection pending = new PendingEffectSelection(playerId, definition.type(), context.sourceId(), EffectCardZone.IN_PLAY, definition.target(), 1, 1, definition.cardFilter(), false, false, definition, candidates);
            context.events().add(new PendingSelectionRequiredEvent(context.state().getGameId(), playerId, definition.type(), context.sourceId(), EffectCardZone.IN_PLAY, definition.target(), 1, 1));
            return new EffectResult(context.state(), pending);
        }
        PokemonInPlay target = EffectStateSupport.pokemonByBenchIndexOrActive(player, definition.targetBenchIndex());
        List<CardInstance> energies = target.getAttachedCards().getEnergies();
        if (energies.isEmpty()) {
            throw new EffectException("Super Potion target must have an attached Energy to discard");
        }
        if (target.getDamageCounters() == 0) {
            throw new EffectException("Super Potion target must have damage to heal");
        }
        if (definition.selectedCardIds().isEmpty() && energies.size() > 1) {
            PendingEffectSelection pending = new PendingEffectSelection(playerId, definition.type(), context.sourceId(), EffectCardZone.ATTACHED, definition.target(), 1, 1, definition.cardFilter(), false, false, definition, energies.stream().map(CardInstance::id).toList());
            context.events().add(new PendingSelectionRequiredEvent(context.state().getGameId(), playerId, definition.type(), context.sourceId(), EffectCardZone.ATTACHED, definition.target(), 1, 1));
            return new EffectResult(context.state(), pending);
        }
        Set<CardInstanceId> selected = new LinkedHashSet<>(definition.selectedCardIds());
        if (selected.isEmpty()) {
            selected.add(energies.get(0).id());
        }
        if (selected.size() != 1) {
            throw new EffectException("Super Potion discards exactly one Energy");
        }
        List<CardInstance> discarded = energies.stream().filter(card -> selected.contains(card.id())).toList();
        if (discarded.size() != 1) {
            throw new EffectException("Selected Energy is not attached to target Pokemon");
        }
        int requestedCounters = definition.amount() / 10;
        int actualCounters = Math.min(requestedCounters, target.getDamageCounters());
        PokemonInPlay updatedTarget = target.withDamageCounters(target.getDamageCounters() - actualCounters).withoutAttachedEnergies(selected);
        BoardState board = EffectStateSupport.withPokemonAtBenchIndexOrActive(player.getBoard(), updatedTarget, definition.targetBenchIndex());
        List<CardInstance> discardCards = new ArrayList<>(player.getDiscardPile().getCards());
        discardCards.addAll(discarded);
        PlayerGameState updatedPlayer = EffectStateSupport.withDiscardAndBoard(player, new DiscardPile(discardCards), board);
        context.events().add(new HealEffectResolvedEvent(context.state().getGameId(), playerId, target.getTopCard().id(), definition.amount(), actualCounters * 10, updatedTarget.getDamageCounters()));
        context.events().add(new EnergyDiscardedEvent(context.state().getGameId(), playerId, target.getTopCard().id(), discarded.stream().map(CardInstance::id).toList()));
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), playerId, definition.type(), context.sourceId()));
        return new EffectResult(EffectStateSupport.withPlayer(context.state(), updatedPlayer));
    }

    private boolean canApplySuperPotion(PokemonInPlay pokemon) {
        return pokemon.getDamageCounters() > 0 && !pokemon.getAttachedCards().getEnergies().isEmpty();
    }
}
