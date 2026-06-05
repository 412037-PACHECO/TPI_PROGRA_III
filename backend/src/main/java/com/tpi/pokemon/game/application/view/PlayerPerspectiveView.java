package com.tpi.pokemon.game.application.view;

public record PlayerPerspectiveView(
        String playerId,
        boolean viewer,
        HandView hand,
        DeckView deck,
        PrizeCardsView prizeCards,
        DiscardPileView discardPile,
        PlayerBoardView board,
        int turnsTaken
) {
}
