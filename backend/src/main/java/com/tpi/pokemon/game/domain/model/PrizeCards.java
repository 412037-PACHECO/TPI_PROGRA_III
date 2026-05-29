package com.tpi.pokemon.game.domain.model;

import java.util.ArrayList;
import java.util.List;

public final class PrizeCards {
    public static final int MAX_PRIZE_CARDS = 6;

    private final List<CardInstance> cards;

    public PrizeCards(List<CardInstance> cards) {
        GameStateValidation.requireUniqueCards(cards, "prize cards");
        if (cards.size() > MAX_PRIZE_CARDS) {
            throw new IllegalArgumentException("PrizeCards size must be between 0 and " + MAX_PRIZE_CARDS);
        }
        this.cards = List.copyOf(cards);
    }

    public static PrizeCards empty() {
        return new PrizeCards(List.of());
    }

    public List<CardInstance> getCards() {
        return cards;
    }

    public int remainingCount() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public PrizeDraw drawUpTo(int requestedCount) {
        if (requestedCount < 0) {
            throw new IllegalArgumentException("requestedCount must not be negative");
        }
        int count = Math.min(requestedCount, cards.size());
        List<CardInstance> taken = new ArrayList<>(cards.subList(0, count));
        List<CardInstance> remaining = new ArrayList<>(cards.subList(count, cards.size()));
        return new PrizeDraw(new PrizeCards(remaining), taken);
    }

    public record PrizeDraw(PrizeCards remainingPrizeCards, List<CardInstance> takenCards) {
        public PrizeDraw {
            java.util.Objects.requireNonNull(remainingPrizeCards, "remainingPrizeCards must not be null");
            java.util.Objects.requireNonNull(takenCards, "takenCards must not be null");
            takenCards = List.copyOf(takenCards);
        }
    }
}
