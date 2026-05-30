package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.GameState;
import java.util.List;
import java.util.Objects;

public final class EffectExecutionService {
    private final EffectRegistry registry;

    public EffectExecutionService() {
        this(EffectRegistry.defaultRegistry());
    }

    public EffectExecutionService(EffectRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context) {
        EffectHandler handler = registry.handlerFor(definition.type());
        return handler.execute(definition, context, this);
    }

    public EffectResult executeAll(List<EffectDefinition> definitions, EffectExecutionContext context) {
        GameState state = context.state();
        EffectExecutionContext current = context;
        for (EffectDefinition definition : definitions) {
            EffectResult result = execute(definition, current);
            state = result.state();
            if (result.pendingSelectionOptional().isPresent()) {
                return result;
            }
            current = current.withState(state);
        }
        return new EffectResult(state);
    }
}
