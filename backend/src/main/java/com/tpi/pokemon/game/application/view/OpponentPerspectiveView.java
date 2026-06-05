package com.tpi.pokemon.game.application.view;

public record OpponentPerspectiveView(
        String playerId,
        HandView hand,
        DeckView deck,
        PrizeCardsView prizeCards,
        DiscardPileView discardPile,
        OpponentBoardView board,
        int turnsTaken
) {
}
