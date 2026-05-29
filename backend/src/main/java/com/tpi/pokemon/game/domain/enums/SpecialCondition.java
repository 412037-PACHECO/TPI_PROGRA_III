package com.tpi.pokemon.game.domain.enums;

public enum SpecialCondition {
    ASLEEP,
    BURNED,
    CONFUSED,
    PARALYZED,
    POISONED;

    public boolean isVolatile() {
        return this == ASLEEP || this == CONFUSED || this == PARALYZED;
    }
}
