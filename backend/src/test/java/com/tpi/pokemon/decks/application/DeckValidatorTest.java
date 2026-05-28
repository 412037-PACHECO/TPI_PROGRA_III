package com.tpi.pokemon.decks.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tpi.pokemon.cards.domain.CardEntity;
import com.tpi.pokemon.cards.domain.CardRepository;
import com.tpi.pokemon.decks.domain.DeckCardEntity;
import com.tpi.pokemon.decks.domain.DeckEntity;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeckValidatorTest {
    @Mock
    private CardRepository cardRepository;

    @Test
    void rejectsDeckWithLessThan60Cards() {
        DeckEntity deck = deckWith(card("xy1-1", 4));
        when(cardRepository.findByCardId("xy1-1")).thenReturn(Optional.of(pokemon("xy1-1", "Pikachu", "[\"Basic\"]")));

        var result = new DeckValidator(cardRepository).validate(deck);

        assertThat(result.valid()).isFalse();
        assertThat(result.totalCards()).isEqualTo(4);
        assertThat(result.errors()).extracting("code").contains("DECK_SIZE_NOT_60");
    }

    @Test
    void rejectsDeckWithMoreThan60Cards() {
        DeckEntity deck = deckWith(card("xy1-1", 4), card("xy1-3", 57));
        when(cardRepository.findByCardId("xy1-1")).thenReturn(Optional.of(pokemon("xy1-1", "Pikachu", "[\"Basic\"]")));
        when(cardRepository.findByCardId("xy1-3")).thenReturn(Optional.of(basicEnergy("xy1-3", "Fire Energy")));

        var result = new DeckValidator(cardRepository).validate(deck);

        assertThat(result.valid()).isFalse();
        assertThat(result.totalCards()).isEqualTo(61);
        assertThat(result.errors()).extracting("code").contains("DECK_SIZE_NOT_60");
    }

    @Test
    void accepts60CardsWithBasicPokemonAndNoCopyLimitViolation() {
        DeckEntity deck = deckWith(card("xy1-1", 4), card("xy1-2", 4), card("xy1-3", 52));
        when(cardRepository.findByCardId("xy1-1")).thenReturn(Optional.of(pokemon("xy1-1", "Pikachu", "[\"Basic\"]")));
        when(cardRepository.findByCardId("xy1-2")).thenReturn(Optional.of(pokemon("xy1-2", "Raichu", "[\"Stage 1\"]")));
        when(cardRepository.findByCardId("xy1-3")).thenReturn(Optional.of(basicEnergy("xy1-3", "Fire Energy")));

        var result = new DeckValidator(cardRepository).validate(deck);

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
        assertThat(result.warnings()).anyMatch(warning -> warning.contains("ACE SPEC"));
    }

    @Test
    void rejectsMoreThan4CopiesByCardName() {
        DeckEntity deck = deckWith(card("xy1-1", 3), card("xy1-99", 2), card("xy1-3", 55));
        when(cardRepository.findByCardId("xy1-1")).thenReturn(Optional.of(pokemon("xy1-1", "Pikachu", "[\"Basic\"]")));
        when(cardRepository.findByCardId("xy1-99")).thenReturn(Optional.of(pokemon("xy1-99", "Pikachu", "[\"Basic\"]")));
        when(cardRepository.findByCardId("xy1-3")).thenReturn(Optional.of(basicEnergy("xy1-3", "Water Energy")));

        var result = new DeckValidator(cardRepository).validate(deck);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).extracting("code").contains("TOO_MANY_COPIES");
    }

    @Test
    void allowsMoreThan4BasicEnergy() {
        DeckEntity deck = deckWith(card("xy1-1", 4), card("xy1-3", 56));
        when(cardRepository.findByCardId("xy1-1")).thenReturn(Optional.of(pokemon("xy1-1", "Pikachu", "[\"Basic\"]")));
        when(cardRepository.findByCardId("xy1-3")).thenReturn(Optional.of(basicEnergy("xy1-3", "Grass Energy")));

        var result = new DeckValidator(cardRepository).validate(deck);

        assertThat(result.errors()).extracting("code").doesNotContain("TOO_MANY_COPIES");
    }

    @Test
    void rejectsDeckWithoutBasicPokemon() {
        DeckEntity deck = deckWith(card("xy1-1", 4), card("xy1-3", 56));
        when(cardRepository.findByCardId("xy1-1")).thenReturn(Optional.of(pokemon("xy1-1", "Raichu", "[\"Stage 1\"]")));
        when(cardRepository.findByCardId("xy1-3")).thenReturn(Optional.of(basicEnergy("xy1-3", "Fire Energy")));

        var result = new DeckValidator(cardRepository).validate(deck);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).extracting("code").contains("NO_BASIC_POKEMON");
    }

    @Test
    void reportsMissingCatalogCardDuringValidation() {
        DeckEntity deck = deckWith(card("xy1-404", 60));
        when(cardRepository.findByCardId("xy1-404")).thenReturn(Optional.empty());

        var result = new DeckValidator(cardRepository).validate(deck);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).extracting("code").contains("CARD_NOT_FOUND", "NO_BASIC_POKEMON");
    }

    @Test
    void rejectsNonXy1CardDuringValidation() {
        DeckEntity deck = deckWith(card("xy1-1", 4), card("base1-1", 56));
        when(cardRepository.findByCardId("xy1-1")).thenReturn(Optional.of(pokemon("xy1-1", "Pikachu", "[\"Basic\"]")));
        when(cardRepository.findByCardId("base1-1")).thenReturn(Optional.of(basicEnergy("base1-1", "Fire Energy", "base1")));

        var result = new DeckValidator(cardRepository).validate(deck);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).extracting("code").contains("NON_XY1_CARD");
    }

    @Test
    void aceSpecIsOnlyWarningNotError() {
        DeckEntity deck = deckWith(card("xy1-1", 4), card("xy1-3", 56));
        when(cardRepository.findByCardId("xy1-1")).thenReturn(Optional.of(pokemon("xy1-1", "Pikachu", "[\"Basic\"]")));
        when(cardRepository.findByCardId("xy1-3")).thenReturn(Optional.of(basicEnergy("xy1-3", "Fire Energy")));

        var result = new DeckValidator(cardRepository).validate(deck);

        assertThat(result.errors()).extracting("code").doesNotContain("ACE_SPEC", "AS_TACTICO");
    }

    private DeckEntity deckWith(DeckCardEntity... cards) {
        DeckEntity deck = new DeckEntity();
        deck.setName("Mazo");
        deck.setOwnerName("Ash");
        for (DeckCardEntity card : cards) {
            deck.addCard(card);
        }
        return deck;
    }

    private DeckCardEntity card(String cardId, int quantity) {
        DeckCardEntity deckCard = new DeckCardEntity();
        deckCard.setCardId(cardId);
        deckCard.setQuantity(quantity);
        return deckCard;
    }

    private CardEntity pokemon(String cardId, String name, String subtypes) {
        CardEntity card = baseCard(cardId, name);
        card.setSupertype("Pokémon");
        card.setSubtypes(subtypes);
        return card;
    }

    private CardEntity basicEnergy(String cardId, String name) {
        return basicEnergy(cardId, name, "xy1");
    }

    private CardEntity basicEnergy(String cardId, String name, String setId) {
        CardEntity card = baseCard(cardId, name);
        card.setSetId(setId);
        card.setSupertype("Energy");
        card.setSubtypes("[\"Basic\"]");
        return card;
    }

    private CardEntity baseCard(String cardId, String name) {
        CardEntity card = new CardEntity();
        card.setCardId(cardId);
        card.setName(name);
        card.setSetId("xy1");
        return card;
    }
}
