package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.SpecialConditionAppliedEvent;

public final class ApplySpecialConditionEffectHandler implements EffectHandler {
    @Override public EffectType type() { return EffectType.APPLY_SPECIAL_CONDITION; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        if (definition.condition() == null) throw new EffectException("Special condition effect requires condition");
        PlayerId ownerId = EffectStateSupport.ownerFor(context, definition.target());
        PokemonInPlay target = EffectStateSupport.activePokemon(context.state(), ownerId);
        PokemonInPlay updated = target.applySpecialCondition(definition.condition());
        context.events().add(new SpecialConditionAppliedEvent(context.state().getGameId(), ownerId, target.getTopCard().id(), definition.condition()));
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), context.actingPlayerId(), definition.type(), context.sourceId()));
        return new EffectResult(EffectStateSupport.withActivePokemon(context.state(), ownerId, updated));
    }
}
