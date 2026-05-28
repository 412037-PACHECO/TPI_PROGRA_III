package com.tpi.pokemon.decks.application;

import com.tpi.pokemon.cards.domain.CardEntity;
import com.tpi.pokemon.cards.domain.CardRepository;
import com.tpi.pokemon.decks.api.CreateDeckRequest;
import com.tpi.pokemon.decks.api.UpdateDeckRequest;
import com.tpi.pokemon.decks.domain.DeckCardEntity;
import com.tpi.pokemon.decks.domain.DeckEntity;
import com.tpi.pokemon.decks.domain.DeckRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeckService {
    private static final String XY1_SET_ID = "xy1";

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;

    public DeckService(DeckRepository deckRepository, CardRepository cardRepository) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
    }

    @Transactional
    public DeckEntity create(CreateDeckRequest request) {
        if (request == null) {
            throw new DeckInvalidOperationException("El cuerpo de creación del mazo es obligatorio");
        }
        DeckEntity deck = new DeckEntity();
        deck.setName(required(request.name(), "El nombre del mazo es obligatorio"));
        deck.setOwnerName(required(request.ownerName(), "El ownerName es obligatorio"));
        return deckRepository.save(deck);
    }

    @Transactional
    public DeckEntity update(Long deckId, UpdateDeckRequest request) {
        if (request == null) {
            throw new DeckInvalidOperationException("El cuerpo de actualización del mazo es obligatorio");
        }
        DeckEntity deck = getDeck(deckId);
        deck.setName(required(request.name(), "El nombre del mazo es obligatorio"));
        deck.setOwnerName(required(request.ownerName(), "El ownerName es obligatorio"));
        return deck;
    }

    @Transactional
    public void delete(Long deckId) {
        DeckEntity deck = getDeck(deckId);
        deckRepository.delete(deck);
    }

    @Transactional(readOnly = true)
    public List<DeckEntity> listByOwner(String ownerName) {
        return deckRepository.findByOwnerNameIgnoreCaseOrderByUpdatedAtDesc(required(ownerName, "El parámetro owner es obligatorio"));
    }

    @Transactional(readOnly = true)
    public DeckEntity getDeck(Long deckId) {
        return deckRepository.findWithCardsById(deckId).orElseThrow(() -> new DeckNotFoundException(deckId));
    }

    @Transactional
    public DeckEntity addOrUpdateCard(Long deckId, String cardId, int quantity) {
        if (quantity < 0) {
            throw new DeckInvalidOperationException("La cantidad no puede ser negativa");
        }

        DeckEntity deck = getDeck(deckId);
        CardEntity card = cardRepository.findByCardId(cardId).orElseThrow(() -> new CatalogCardNotFoundException(cardId));
        ensureXy1(card);

        deck.findCard(cardId).ifPresentOrElse(existing -> {
            if (quantity == 0) {
                deck.removeCard(existing);
            } else {
                existing.setQuantity(quantity);
            }
        }, () -> {
            if (quantity > 0) {
                DeckCardEntity deckCard = new DeckCardEntity();
                deckCard.setCardId(card.getCardId());
                deckCard.setQuantity(quantity);
                deck.addCard(deckCard);
            }
        });
        return deck;
    }

    @Transactional
    public DeckEntity removeCard(Long deckId, String cardId) {
        DeckEntity deck = getDeck(deckId);
        DeckCardEntity card = deck.findCard(cardId).orElseThrow(() -> new DeckCardNotFoundException(deckId, cardId));
        deck.removeCard(card);
        return deck;
    }

    private void ensureXy1(CardEntity card) {
        if (!XY1_SET_ID.equalsIgnoreCase(card.getSetId())) {
            throw new DeckInvalidOperationException("Solo se pueden agregar cartas del set xy1 al Deck Builder de esta fase");
        }
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new DeckInvalidOperationException(message);
        }
        return value.trim();
    }
}
