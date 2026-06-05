package com.tpi.pokemon.game.application.view;

public record TurnView(
        String currentPlayerId,
        String startingPlayerId,
        int turnNumber,
        String phase,
        boolean cardDrawnThisTurn,
        boolean energyAttachedThisTurn,
        boolean supporterPlayedThisTurn,
        boolean stadiumPlayedThisTurn,
        boolean retreatedThisTurn
) {
}
