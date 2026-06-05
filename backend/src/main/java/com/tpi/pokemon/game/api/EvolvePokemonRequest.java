package com.tpi.pokemon.game.api;

public record EvolvePokemonRequest(String playerId, String evolutionCardInstanceId, PokemonTargetRequest target) {
}
