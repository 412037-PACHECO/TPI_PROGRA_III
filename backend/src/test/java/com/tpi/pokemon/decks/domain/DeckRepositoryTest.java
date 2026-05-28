package com.tpi.pokemon.decks.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class DeckRepositoryTest {
    @Autowired
    private DeckRepository repository;

    @Test
    void savesDeckCardsAndFindsByOwnerIgnoringCase() {
        DeckEntity deck = new DeckEntity();
        deck.setName("Starter");
        deck.setOwnerName("Ash");

        DeckCardEntity card = new DeckCardEntity();
        card.setCardId("xy1-1");
        card.setQuantity(2);
        deck.addCard(card);
        repository.saveAndFlush(deck);

        assertThat(repository.findByOwnerNameIgnoreCaseOrderByUpdatedAtDesc("ash")).hasSize(1);
        DeckEntity loaded = repository.findWithCardsById(deck.getId()).orElseThrow();
        assertThat(loaded.getCards()).hasSize(1);
        assertThat(loaded.getCards().getFirst().getCardId()).isEqualTo("xy1-1");
    }
}
