package com.tpi.pokemon.decks.application;

import com.tpi.pokemon.cards.domain.CardEntity;
import com.tpi.pokemon.cards.domain.CardRepository;
import com.tpi.pokemon.decks.api.DeckCardResponse;
import com.tpi.pokemon.decks.api.DeckDetailResponse;
import com.tpi.pokemon.decks.api.DeckResponse;
import com.tpi.pokemon.decks.domain.DeckCardEntity;
import com.tpi.pokemon.decks.domain.DeckEntity;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DeckMapper {
    private final CardRepository cardRepository;

    public DeckMapper(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public DeckResponse toResponse(DeckEntity deck) {
        return new DeckResponse(deck.getId(), deck.getName(), deck.getOwnerName(), totalCards(deck), deck.getCreatedAt(), deck.getUpdatedAt());
    }

    public DeckDetailResponse toDetailResponse(DeckEntity deck) {
        List<DeckCardResponse> cards = deck.getCards().stream()
                .sorted(Comparator.comparing(DeckCardEntity::getCardId))
                .map(this::toCardResponse)
                .toList();
        return new DeckDetailResponse(deck.getId(), deck.getName(), deck.getOwnerName(), totalCards(deck), deck.getCreatedAt(), deck.getUpdatedAt(), cards);
    }

    private DeckCardResponse toCardResponse(DeckCardEntity deckCard) {
        CardEntity card = cardRepository.findByCardId(deckCard.getCardId())
                .orElseThrow(() -> new CatalogCardNotFoundException(deckCard.getCardId()));
        return new DeckCardResponse(card.getCardId(), card.getName(), card.getSupertype(), card.getSubtypes(), card.getImageSmall(), deckCard.getQuantity());
    }

    private int totalCards(DeckEntity deck) {
        return deck.getCards().stream().mapToInt(DeckCardEntity::getQuantity).sum();
    }
}
