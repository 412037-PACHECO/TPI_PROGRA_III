package com.tpi.pokemon.game.api;

import java.util.List;

public record ChooseInitialPokemonRequest(String playerId, String activePokemonId, List<String> benchPokemonIds) {
}
