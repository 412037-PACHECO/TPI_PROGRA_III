package com.tpi.pokemon.game.engine.effect;

public interface EffectHandler {
    EffectType type();

    EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService);
}
