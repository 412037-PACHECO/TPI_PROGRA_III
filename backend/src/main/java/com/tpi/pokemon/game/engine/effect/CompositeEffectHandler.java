package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;

public final class CompositeEffectHandler implements EffectHandler {
    @Override public EffectType type() { return EffectType.COMPOSITE; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        EffectResult result = executionService.executeAll(definition.children(), context);
        context.events().add(new EffectResolvedEvent(result.state().getGameId(), context.actingPlayerId(), definition.type(), context.sourceId()));
        return result;
    }
}
