package com.tpi.pokemon.game.api;

public record PlayTrainerRequest(
        String playerId,
        String trainerCardInstanceId,
        PokemonTargetRequest target
) {}
