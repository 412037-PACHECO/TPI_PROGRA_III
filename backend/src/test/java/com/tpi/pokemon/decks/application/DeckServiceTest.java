package com.tpi.pokemon.decks.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpi.pokemon.cards.domain.CardEntity;
import com.tpi.pokemon.cards.domain.CardRepository;
import com.tpi.pokemon.decks.api.CreateDeckRequest;
import com.tpi.pokemon.decks.domain.DeckEntity;
import com.tpi.pokemon.decks.domain.DeckRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {
    @Mock
    private DeckRepository deckRepository;
    @Mock
    private CardRepository cardRepository;

    @Test
    void createsEmptyDeck() {
        when(deckRepository.save(any(DeckEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeckEntity deck = service().create(new CreateDeckRequest("Starter", "Ash"));

        assertThat(deck.getName()).isEqualTo("Starter");
        assertThat(deck.getOwnerName()).isEqualTo("Ash");
        assertThat(deck.getCards()).isEmpty();
    }

    @Test
    void listsDecksByOwnerIgnoringCase() {
        when(deckRepository.findByOwnerNameIgnoreCaseOrderByUpdatedAtDesc("Ash")).thenReturn(List.of(new DeckEntity()));

        assertThat(service().listByOwner(" Ash ")).hasSize(1);

        verify(deckRepository).findByOwnerNameIgnoreCaseOrderByUpdatedAtDesc("Ash");
    }

    @Test
    void addsExistingXy1Card() {
        DeckEntity deck = deck();
        when(deckRepository.findWithCardsById(1L)).thenReturn(Optional.of(deck));
        when(cardRepository.findByCardId("xy1-1")).thenReturn(Optional.of(card("xy1-1", "xy1")));

        DeckEntity updated = service().addOrUpdateCard(1L, "xy1-1", 2);

        assertThat(updated.getCards()).hasSize(1);
        assertThat(updated.getCards().getFirst().getQuantity()).isEqualTo(2);
    }

    @Test
    void rejectsUnknownCard() {
        when(deckRepository.findWithCardsById(1L)).thenReturn(Optional.of(deck()));
        when(cardRepository.findByCardId("xy1-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().addOrUpdateCard(1L, "xy1-404", 1))
                .isInstanceOf(CatalogCardNotFoundException.class);
    }

    @Test
    void updatesQuantityAndZeroRemovesCard() {
        DeckEntity deck = deck();
        when(deckRepository.findWithCardsById(1L)).thenReturn(Optional.of(deck));
        when(cardRepository.findByCardId("xy1-1")).thenReturn(Optional.of(card("xy1-1", "xy1")));

        service().addOrUpdateCard(1L, "xy1-1", 3);
        service().addOrUpdateCard(1L, "xy1-1", 4);
        assertThat(deck.getCards().getFirst().getQuantity()).isEqualTo(4);

        service().addOrUpdateCard(1L, "xy1-1", 0);
        assertThat(deck.getCards()).isEmpty();
    }

    @Test
    void rejectsNegativeQuantityAndNonXy1Cards() {
        assertThatThrownBy(() -> service().addOrUpdateCard(1L, "xy1-1", -1))
                .isInstanceOf(DeckInvalidOperationException.class);

        when(deckRepository.findWithCardsById(1L)).thenReturn(Optional.of(deck()));
        when(cardRepository.findByCardId("base1-1")).thenReturn(Optional.of(card("base1-1", "base1")));
        assertThatThrownBy(() -> service().addOrUpdateCard(1L, "base1-1", 1))
                .isInstanceOf(DeckInvalidOperationException.class)
                .hasMessageContaining("xy1");
    }

    private DeckService service() {
        return new DeckService(deckRepository, cardRepository);
    }

    private DeckEntity deck() {
        DeckEntity deck = new DeckEntity();
        deck.setName("Starter");
        deck.setOwnerName("Ash");
        return deck;
    }

    private CardEntity card(String cardId, String setId) {
        CardEntity card = new CardEntity();
        card.setCardId(cardId);
        card.setName("Pikachu");
        card.setSetId(setId);
        return card;
    }
}
