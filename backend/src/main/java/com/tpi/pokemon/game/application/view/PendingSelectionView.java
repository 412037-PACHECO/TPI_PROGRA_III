package com.tpi.pokemon.game.application.view;

import java.util.List;

public record PendingSelectionView(
        boolean pending,
        boolean viewerMustChoose,
        String playerId,
        String effectType,
        String sourceId,
        String sourceZone,
        String target,
        int minSelections,
        int maxSelections,
        boolean revealSelectedCards,
        boolean requiresShuffle,
        List<String> candidateCardIds
) {
}
