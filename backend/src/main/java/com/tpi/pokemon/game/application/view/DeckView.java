package com.tpi.pokemon.game.application.view;

import java.util.List;

public record DeckView(int count, boolean orderVisible, List<CardView> cards) {
}
