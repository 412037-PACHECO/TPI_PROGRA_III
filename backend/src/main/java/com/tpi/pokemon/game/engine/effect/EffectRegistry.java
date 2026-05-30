package com.tpi.pokemon.game.engine.effect;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class EffectRegistry {
    private final Map<EffectType, EffectHandler> handlers;

    public EffectRegistry(Collection<EffectHandler> handlers) {
        this.handlers = new EnumMap<>(EffectType.class);
        for (EffectHandler handler : handlers) {
            this.handlers.put(handler.type(), handler);
        }
    }

    public static EffectRegistry defaultRegistry() {
        return new EffectRegistry(java.util.List.of(
                new DealDamageEffectHandler(),
                new HealDamageEffectHandler(),
                new ApplySpecialConditionEffectHandler(),
                new DrawCardsEffectHandler(),
                new DiscardAttachedEnergyEffectHandler(),
                new SearchDeckEffectHandler(),
                new ShuffleDeckEffectHandler(),
                new DiscardCardsEffectHandler(),
                new AttachEnergyEffectHandler(),
                new MoveEnergyEffectHandler(),
                new SwitchActiveEffectHandler(),
                new PlaceDamageCountersEffectHandler(),
                new CoinFlipEffectHandler(),
                new CompositeEffectHandler()
        ));
    }

    public Optional<EffectHandler> findHandler(EffectType type) {
        return Optional.ofNullable(handlers.get(type));
    }

    public EffectHandler handlerFor(EffectType type) {
        return findHandler(type).orElseThrow(() -> new EffectException("No handler registered for effect type " + type));
    }
}
