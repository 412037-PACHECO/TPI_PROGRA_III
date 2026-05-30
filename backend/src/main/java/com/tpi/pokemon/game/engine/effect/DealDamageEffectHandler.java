package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.DamageAppliedEvent;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;

public final class DealDamageEffectHandler implements EffectHandler {
    @Override public EffectType type() { return EffectType.DEAL_DAMAGE; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        PlayerId ownerId = EffectStateSupport.ownerFor(context, definition.target());
        PokemonInPlay target = EffectStateSupport.activePokemon(context.state(), ownerId);
        PokemonInPlay damaged = target.applyDamage(definition.amount());
        context.events().add(new DamageAppliedEvent(context.state().getGameId(), target.getTopCard().id(), definition.amount(), definition.amount() / 10, damaged.getDamageCounters()));
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), context.actingPlayerId(), definition.type(), context.sourceId()));
        return new EffectResult(EffectStateSupport.withActivePokemon(context.state(), ownerId, damaged));
    }
}
