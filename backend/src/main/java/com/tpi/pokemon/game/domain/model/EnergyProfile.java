package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.enums.EnergyType;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record EnergyProfile(List<EnergyType> provides) {
    public EnergyProfile {
        Objects.requireNonNull(provides, "provides must not be null");
        if (provides.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("provides must not contain null values");
        }
        provides = List.copyOf(provides);
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
}
