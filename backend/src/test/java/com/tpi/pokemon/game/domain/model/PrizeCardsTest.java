package com.tpi.pokemon.game.domain.model;

import static com.tpi.pokemon.game.GameStateFixtures.card;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class PrizeCardsTest {
    @Test
    void acceptsSixPrizeCardsForInitialState() {
        PrizeCards prizes = new PrizeCards(List.of(
                card("prize-1"), card("prize-2"), card("prize-3"),
                card("prize-4"), card("prize-5"), card("prize-6")
        ));

        assertThat(prizes.getCards()).hasSize(6);
    }

    @Test
    void acceptsOnePrizeCardNearGameEnd() {
        PrizeCards prizes = new PrizeCards(List.of(card("last-prize")));

        assertThat(prizes.getCards()).hasSize(1);
    }

    @Test
    void acceptsZeroPrizeCardsForUninitializedState() {
        PrizeCards prizes = PrizeCards.empty();

        assertThat(prizes.getCards()).isEmpty();
    }

    @Test
    void rejectsUnsupportedPrizeCardSizes() {
        assertThatThrownBy(() -> new PrizeCards(List.of(card("prize-1"), card("prize-2"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0, 1, or 6");
    }
}
