package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.EnergyMovedEvent;
import com.tpi.pokemon.game.engine.event.PendingSelectionRequiredEvent;
import java.util.List;
import java.util.Set;

public final class MoveEnergyEffectHandler implements EffectHandler {
    @Override public EffectType type() { return EffectType.MOVE_ENERGY; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        PlayerId ownerId = EffectStateSupport.ownerFor(context, definition.target());
        if (definition.selectedCardIds().isEmpty()) {
            PendingEffectSelection pending = new PendingEffectSelection(ownerId, definition.type(), context.sourceId(), EffectCardZone.ATTACHED, definition.target(), 1, definition.amount(), definition.cardFilter());
            context.events().add(new PendingSelectionRequiredEvent(context.state().getGameId(), ownerId, definition.type(), context.sourceId(), EffectCardZone.ATTACHED, definition.target(), 1, definition.amount()));
            return new EffectResult(context.state(), pending);
        }
        PlayerGameState owner = EffectStateSupport.playerState(context.state(), ownerId);
        if (definition.sourceBenchIndex() == definition.destinationBenchIndex()) {
            throw new EffectException("Source and destination Pokemon must be different for moving energy");
        }
        PokemonInPlay source = EffectStateSupport.pokemonByBenchIndexOrActive(owner, definition.sourceBenchIndex());
        PokemonInPlay destination = EffectStateSupport.pokemonByBenchIndexOrActive(owner, definition.destinationBenchIndex());
        List<CardInstance> selected = EffectCardMovementSupport.selectedFrom(source.getAttachedCards().getEnergies(), definition.selectedCardIds(), "ATTACHED");
        PokemonInPlay updatedSource = source.withoutAttachedEnergies(Set.copyOf(definition.selectedCardIds()));
        PokemonInPlay updatedDestination = destination;
        for (CardInstance energy : selected) {
            updatedDestination = updatedDestination.withAttachedEnergy(energy);
        }
        BoardState board = owner.getBoard();
        board = EffectStateSupport.withPokemonAtBenchIndexOrActive(board, updatedSource, definition.sourceBenchIndex());
        board = EffectStateSupport.withPokemonAtBenchIndexOrActive(board, updatedDestination, definition.destinationBenchIndex());
        PlayerGameState updatedOwner = owner.withBoard(board);
        context.events().add(new EnergyMovedEvent(context.state().getGameId(), ownerId, definition.selectedCardIds(), source.getTopCard().id(), destination.getTopCard().id()));
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), context.actingPlayerId(), definition.type(), context.sourceId()));
        return new EffectResult(EffectStateSupport.withPlayer(context.state(), updatedOwner));
    }
}
