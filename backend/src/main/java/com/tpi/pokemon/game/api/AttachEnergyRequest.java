package com.tpi.pokemon.game.api;

public record AttachEnergyRequest(String playerId, String energyCardInstanceId, PokemonTargetRequest target) {
}
