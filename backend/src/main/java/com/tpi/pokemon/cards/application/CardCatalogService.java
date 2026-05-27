package com.tpi.pokemon.cards.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.tpi.pokemon.cards.domain.CardEntity;
import com.tpi.pokemon.cards.domain.CardRepository;
import com.tpi.pokemon.cards.infrastructure.PokemonTcgApiClient;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardCatalogService {
    public static final String XY1_SET_ID = "xy1";

    private final CardRepository repository;
    private final PokemonTcgApiClient apiClient;
    private final CardMapper mapper;

    public CardCatalogService(CardRepository repository, PokemonTcgApiClient apiClient, CardMapper mapper) {
        this.repository = repository;
        this.apiClient = apiClient;
        this.mapper = mapper;
    }

    @Transactional
    public CardImportSummary importXy1() {
        List<JsonNode> cards = apiClient.fetchCardsBySet(XY1_SET_ID);
        int created = 0;
        int updated = 0;
        int skipped = 0;
        int errors = 0;

        for (JsonNode cardJson : cards) {
            try {
                String cardId = cardJson.path("id").asText(null);
                if (cardId == null || cardId.isBlank()) {
                    skipped++;
                    continue;
                }
                CardEntity entity = repository.findByCardId(cardId).orElse(null);
                if (entity == null) {
                    repository.save(mapper.fromApiJson(cardJson));
                    created++;
                } else {
                    mapper.copyApiJsonInto(entity, cardJson);
                    repository.save(entity);
                    updated++;
                }
            } catch (RuntimeException e) {
                errors++;
            }
        }

        return new CardImportSummary(cards.size(), created, updated, skipped, errors);
    }

    @Transactional(readOnly = true)
    public Page<CardEntity> findCards(String setId, String name, Pageable pageable) {
        boolean hasSetId = setId != null && !setId.isBlank();
        boolean hasName = name != null && !name.isBlank();
        if (hasSetId && hasName) {
            return repository.findBySetIdIgnoreCaseAndNameContainingIgnoreCase(setId, name, pageable);
        }
        if (hasSetId) {
            return repository.findBySetIdIgnoreCase(setId, pageable);
        }
        if (hasName) {
            return repository.findByNameContainingIgnoreCase(name, pageable);
        }
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public CardEntity getByCardId(String cardId) {
        return repository.findByCardId(cardId).orElseThrow(() -> new CardNotFoundException(cardId));
    }
}
