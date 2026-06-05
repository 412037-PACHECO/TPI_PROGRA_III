package com.tpi.pokemon.game.application.view;

import java.util.List;
import java.util.Set;

public record CardView(
        String instanceId,
        String cardId,
        String name,
        String supertype,
        Set<String> subtypes,
        Integer hp,
        List<String> pokemonTypes,
        Integer retreatCost
) {
}
