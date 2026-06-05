package com.tpi.pokemon.game.api;

public record StartGameRequest(String playerId, Long playerOneDeckId, Long playerTwoDeckId) {
}
