package com.tpi.pokemon.game.application.view;

public record GameViewResponse(
        String gameId,
        String status,
        String viewerPlayerId,
        PlayerPerspectiveView player,
        OpponentPerspectiveView opponent,
        TurnView turn,
        StadiumView stadium,
        PendingSelectionView pendingSelection,
        String winnerId,
        String finishType,
        String finishReason
) {
}
