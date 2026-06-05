package com.tpi.pokemon.game.api;

import java.util.List;

public record ResolveSelectionRequest(
        String playerId,
        String selectionId,
        List<String> selectedCardIds,
        PokemonTargetRequest target
) {}
