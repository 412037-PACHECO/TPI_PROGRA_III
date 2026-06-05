package com.tpi.pokemon.game.application.view;

import java.util.List;

public record PrizeCardsView(int remainingCount, boolean cardsVisible, List<CardView> cards) {
}
