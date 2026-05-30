package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.HealEffectResolvedEvent;

public final class HealDamageEffectHandler implements EffectHandler {
    @Override public EffectType type() { return EffectType.HEAL_DAMAGE; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        PlayerId ownerId = EffectStateSupport.ownerFor(context, definition.target());
        PokemonInPlay target = EffectStateSupport.activePokemon(context.state(), ownerId);
        int requestedCounters = definition.amount() / 10;
        int actualCounters = Math.min(requestedCounters, target.getDamageCounters());
        PokemonInPlay healed = target.withDamageCounters(target.getDamageCounters() - actualCounters);
        context.events().add(new HealEffectResolvedEvent(context.state().getGameId(), ownerId, target.getTopCard().id(), definition.amount(), actualCounters * 10, healed.getDamageCounters()));
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), context.actingPlayerId(), definition.type(), context.sourceId()));
        return new EffectResult(EffectStateSupport.withActivePokemon(context.state(), ownerId, healed));
    }
}
