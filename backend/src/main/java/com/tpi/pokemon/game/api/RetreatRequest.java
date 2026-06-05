package com.tpi.pokemon.game.api;

import java.util.List;

public record RetreatRequest(String playerId, Integer benchIndex, List<String> energyCardInstanceIdsToDiscard) {
}
