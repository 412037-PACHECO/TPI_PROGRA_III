package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import java.util.Objects;
import java.util.Optional;

public record SpecialConditionSet(SpecialCondition volatileCondition, boolean burned, boolean poisoned) {
    public SpecialConditionSet {
        if (volatileCondition != null && !volatileCondition.isVolatile()) {
            throw new IllegalArgumentException("volatileCondition must be ASLEEP, CONFUSED or PARALYZED");
        }
    }

    public static SpecialConditionSet none() {
        return new SpecialConditionSet(null, false, false);
    }

    public Optional<SpecialCondition> getVolatileCondition() {
        return Optional.ofNullable(volatileCondition);
    }

    public boolean has(SpecialCondition condition) {
        Objects.requireNonNull(condition, "condition must not be null");
        return switch (condition) {
            case ASLEEP, CONFUSED, PARALYZED -> condition == volatileCondition;
            case BURNED -> burned;
            case POISONED -> poisoned;
        };
    }

    public boolean hasAny() {
        return volatileCondition != null || burned || poisoned;
    }

    public SpecialConditionSet apply(SpecialCondition condition) {
        Objects.requireNonNull(condition, "condition must not be null");
        return switch (condition) {
            case ASLEEP, CONFUSED, PARALYZED -> new SpecialConditionSet(condition, burned, poisoned);
            case BURNED -> new SpecialConditionSet(volatileCondition, true, poisoned);
            case POISONED -> new SpecialConditionSet(volatileCondition, burned, true);
        };
    }

    public SpecialConditionSet remove(SpecialCondition condition) {
        Objects.requireNonNull(condition, "condition must not be null");
        return switch (condition) {
            case ASLEEP, CONFUSED, PARALYZED -> condition == volatileCondition ? new SpecialConditionSet(null, burned, poisoned) : this;
            case BURNED -> new SpecialConditionSet(volatileCondition, false, poisoned);
            case POISONED -> new SpecialConditionSet(volatileCondition, burned, false);
        };
    }
}
