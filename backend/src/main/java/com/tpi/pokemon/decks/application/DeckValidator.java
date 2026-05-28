package com.tpi.pokemon.decks.application;

import com.tpi.pokemon.cards.domain.CardEntity;
import com.tpi.pokemon.cards.domain.CardRepository;
import com.tpi.pokemon.decks.api.DeckValidationError;
import com.tpi.pokemon.decks.api.DeckValidationResponse;
import com.tpi.pokemon.decks.domain.DeckCardEntity;
import com.tpi.pokemon.decks.domain.DeckEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class DeckValidator {
    static final String XY1_SET_ID = "xy1";
    private static final int REQUIRED_DECK_SIZE = 60;
    private static final int MAX_COPIES_BY_NAME = 4;

    private final CardRepository cardRepository;

    public DeckValidator(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public DeckValidationResponse validate(DeckEntity deck) {
        List<DeckValidationError> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, CopyCounter> copiesByName = new HashMap<>();
        int totalCards = 0;
        boolean hasBasicPokemon = false;

        for (DeckCardEntity deckCard : deck.getCards()) {
            totalCards += deckCard.getQuantity();
            Optional<CardEntity> optionalCard = cardRepository.findByCardId(deckCard.getCardId());
            if (optionalCard.isEmpty()) {
                errors.add(new DeckValidationError("CARD_NOT_FOUND", deckCard.getCardId(), null, "La carta no existe en el catálogo local"));
                continue;
            }

            CardEntity card = optionalCard.get();
            if (!XY1_SET_ID.equalsIgnoreCase(card.getSetId())) {
                errors.add(new DeckValidationError("NON_XY1_CARD", card.getCardId(), card.getName(), "Solo se permiten cartas del set xy1"));
            }
            if (isBasicPokemon(card)) {
                hasBasicPokemon = true;
            }
            if (!isBasicEnergy(card)) {
                String key = normalize(card.getName());
                copiesByName.computeIfAbsent(key, ignored -> new CopyCounter(card.getCardId(), card.getName())).add(deckCard.getQuantity());
            }
        }

        if (totalCards != REQUIRED_DECK_SIZE) {
            errors.add(new DeckValidationError("DECK_SIZE_NOT_60", null, null, "El mazo debe tener exactamente 60 cartas"));
        }
        copiesByName.values().stream()
                .filter(counter -> counter.quantity > MAX_COPIES_BY_NAME)
                .forEach(counter -> errors.add(new DeckValidationError("TOO_MANY_COPIES", counter.cardId, counter.cardName, "Máximo 4 copias por nombre de carta, salvo Energía Básica")));
        if (!hasBasicPokemon) {
            errors.add(new DeckValidationError("NO_BASIC_POKEMON", null, null, "El mazo debe incluir al menos 1 Pokémon Básico"));
        }

        warnings.add("AS TÁCTICO / ACE SPEC no se valida como regla obligatoria para mazos xy1.");
        return new DeckValidationResponse(errors.isEmpty(), totalCards, errors, warnings);
    }

    private boolean isBasicPokemon(CardEntity card) {
        return containsIgnoreCase(card.getSupertype(), "Pokémon") && containsSubtype(card, "Basic", "Básica", "Basica");
    }

    private boolean isBasicEnergy(CardEntity card) {
        if (!containsIgnoreCase(card.getSupertype(), "Energy") && !containsIgnoreCase(card.getSupertype(), "Energía") && !containsIgnoreCase(card.getSupertype(), "Energia")) {
            return false;
        }
        if (containsSubtype(card, "Basic", "Básica", "Basica")) {
            return true;
        }
        String name = normalize(card.getName());
        return name.equals("grass energy") || name.equals("fire energy") || name.equals("water energy") || name.equals("lightning energy")
                || name.equals("psychic energy") || name.equals("fighting energy") || name.equals("darkness energy") || name.equals("metal energy")
                || name.equals("fairy energy") || name.equals("energia planta") || name.equals("energia fuego") || name.equals("energia agua")
                || name.equals("energia rayo") || name.equals("energia psiquica") || name.equals("energia lucha") || name.equals("energia oscuridad")
                || name.equals("energia metal") || name.equals("energia hada");
    }

    private boolean containsSubtype(CardEntity card, String... candidates) {
        for (String candidate : candidates) {
            if (containsIgnoreCase(card.getSubtypes(), candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(String source, String candidate) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(candidate.toLowerCase(Locale.ROOT));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u").trim();
    }

    private static final class CopyCounter {
        private final String cardId;
        private final String cardName;
        private int quantity;

        private CopyCounter(String cardId, String cardName) {
            this.cardId = cardId;
            this.cardName = cardName;
        }

        private void add(int quantity) {
            this.quantity += quantity;
        }
    }
}
