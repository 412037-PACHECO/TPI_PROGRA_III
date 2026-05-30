package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.ActivePokemon;
import com.tpi.pokemon.game.domain.model.Bench;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.ActivePokemonSwitchedEvent;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.PendingSelectionRequiredEvent;
import java.util.ArrayList;
import java.util.List;

public final class SwitchActiveEffectHandler implements EffectHandler {
    @Override public EffectType type() { return EffectType.SWITCH_ACTIVE; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        PlayerId ownerId = EffectStateSupport.ownerFor(context, definition.target());
        PlayerGameState owner = EffectStateSupport.playerState(context.state(), ownerId);
        if (definition.targetBenchIndex() < 0) {
            PendingEffectSelection pending = new PendingEffectSelection(ownerId, definition.type(), context.sourceId(), null, definition.target(), 1, 1, CardFilterSpec.any());
            context.events().add(new PendingSelectionRequiredEvent(context.state().getGameId(), ownerId, definition.type(), context.sourceId(), null, definition.target(), 1, 1));
            return new EffectResult(context.state(), pending);
        }
        PokemonInPlay active = owner.getBoard().getActivePokemon().map(ActivePokemon::getPokemon).orElseThrow(() -> new EffectException("Cannot switch without active Pokemon"));
        if (definition.targetBenchIndex() >= owner.getBoard().getBench().getPokemon().size()) {
            throw new EffectException("bench index out of range: " + definition.targetBenchIndex());
        }
        List<PokemonInPlay> bench = new ArrayList<>(owner.getBoard().getBench().getPokemon());
        PokemonInPlay newActive = bench.remove(definition.targetBenchIndex());
        bench.add(definition.targetBenchIndex(), active);
        BoardState board = new BoardState(new ActivePokemon(newActive), new Bench(bench), owner.getBoard().getActiveStadium().orElse(null));
        PlayerGameState updatedOwner = owner.withBoard(board);
        context.events().add(new ActivePokemonSwitchedEvent(context.state().getGameId(), ownerId, active.getTopCard().id(), newActive.getTopCard().id()));
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), context.actingPlayerId(), definition.type(), context.sourceId()));
        return new EffectResult(EffectStateSupport.withPlayer(context.state(), updatedOwner));
    }
}
