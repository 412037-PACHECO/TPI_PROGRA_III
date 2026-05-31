package com.tpi.pokemon.game.engine.effect.reactive;

import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.engine.event.GameEvent;
import java.util.List;
import java.util.Objects;

public record ReactiveEffectContext(
        GameState state,
        ReactiveEffectTrigger trigger,
        DamageReceivedContext damageReceived,
        List<GameEvent> events
) {
    public ReactiveEffectContext {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(trigger, "trigger must not be null");
        Objects.requireNonNull(damageReceived, "damageReceived must not be null");
        Objects.requireNonNull(events, "events must not be null");
    }
}
