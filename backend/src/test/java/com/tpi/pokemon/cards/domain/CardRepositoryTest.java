package com.tpi.pokemon.cards.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class CardRepositoryTest {
    @Autowired
    private CardRepository repository;

    @Test
    void searchesByCardIdSetIdAndPartialNameIgnoringCase() {
        CardEntity card = new CardEntity();
        card.setCardId("xy1-1");
        card.setName("Venusaur-EX");
        card.setSetId("xy1");
        repository.save(card);

        assertThat(repository.findByCardId("xy1-1")).isPresent();
        assertThat(repository.findBySetIdIgnoreCase("XY1", PageRequest.of(0, 10))).hasSize(1);
        assertThat(repository.findByNameContainingIgnoreCase("venus", PageRequest.of(0, 10))).hasSize(1);
        assertThat(repository.findBySetIdIgnoreCaseAndNameContainingIgnoreCase("xy1", "EX", PageRequest.of(0, 10))).hasSize(1);
    }
}
