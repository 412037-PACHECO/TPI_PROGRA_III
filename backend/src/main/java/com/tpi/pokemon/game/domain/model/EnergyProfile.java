package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.enums.EnergyType;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record EnergyProfile(List<EnergyType> provides, boolean providesAnyTypeWhileAttached, int attachDamageCountersFromHand) {
    public EnergyProfile {
        Objects.requireNonNull(provides, "provides must not be null");
        if (provides.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("provides must not contain null values");
        }
        if (attachDamageCountersFromHand < 0) {
            throw new IllegalArgumentException("attachDamageCountersFromHand must not be negative");
        }
        provides = List.copyOf(provides);
    }

    public EnergyProfile(List<EnergyType> provides) {
        this(provides, false, 0);
    }

    public static EnergyProfile none() {
        return new EnergyProfile(List.of());
    }

    public static EnergyProfile of(EnergyType... provides) {
        Objects.requireNonNull(provides, "provides must not be null");
        return new EnergyProfile(Arrays.asList(provides));
    }

    public static EnergyProfile basic(EnergyType type) {
        return of(type);
    }

    public static EnergyProfile rainbow() {
        return new EnergyProfile(List.of(EnergyType.COLORLESS), true, 1);
    }

    public boolean canProvide(EnergyType type) {
        Objects.requireNonNull(type, "type must not be null");
        return provides.contains(type) || providesAnyTypeWhileAttached;
    }
}
