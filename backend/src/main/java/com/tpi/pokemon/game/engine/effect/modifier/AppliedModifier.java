package com.tpi.pokemon.game.engine.effect.modifier;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import java.util.Objects;

public record AppliedModifier(
        CardInstanceId sourceCardId,
        String effectId,
        ModifierType type,
        ModifierOperation operation,
        ModifierLayer layer,
        int amount,
        int valueBefore,
        int valueAfter
) {
    public AppliedModifier {
        Objects.requireNonNull(sourceCardId, "sourceCardId must not be null");
        if (effectId == null || effectId.isBlank()) {
            throw new IllegalArgumentException("effectId must not be blank");
        }
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(operation, "operation must not be null");
        Objects.requireNonNull(layer, "layer must not be null");
    }
}
