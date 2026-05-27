package com.tpi.pokemon.cards.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpi.pokemon.cards.api.CardResponse;
import com.tpi.pokemon.cards.domain.CardEntity;
import org.springframework.stereotype.Component;

@Component
public class CardMapper {
    private final ObjectMapper objectMapper;

    public CardMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CardEntity fromApiJson(JsonNode cardJson) {
        CardEntity entity = new CardEntity();
        copyApiJsonInto(entity, cardJson);
        return entity;
    }

    public void copyApiJsonInto(CardEntity entity, JsonNode cardJson) {
        entity.setCardId(text(cardJson, "id"));
        entity.setName(text(cardJson, "name"));
        entity.setSupertype(text(cardJson, "supertype"));
        entity.setSubtypes(json(cardJson.get("subtypes")));
        entity.setSetId(text(cardJson.path("set"), "id"));
        entity.setSetName(text(cardJson.path("set"), "name"));
        entity.setNumber(text(cardJson, "number"));
        entity.setRarity(text(cardJson, "rarity"));
        entity.setHp(text(cardJson, "hp"));
        entity.setTypes(json(cardJson.get("types")));
        entity.setEvolvesFrom(text(cardJson, "evolvesFrom"));
        entity.setRules(json(cardJson.get("rules")));
        entity.setAttacks(json(cardJson.get("attacks")));
        entity.setAbilities(json(cardJson.get("abilities")));
        entity.setWeaknesses(json(cardJson.get("weaknesses")));
        entity.setResistances(json(cardJson.get("resistances")));
        entity.setRetreatCost(json(cardJson.get("retreatCost")));
        entity.setConvertedRetreatCost(cardJson.hasNonNull("convertedRetreatCost") ? cardJson.get("convertedRetreatCost").asInt() : null);
        entity.setImageSmall(text(cardJson.path("images"), "small"));
        entity.setImageLarge(text(cardJson.path("images"), "large"));
        entity.setRawJson(json(cardJson));
    }

    public CardResponse toResponse(CardEntity entity) {
        return new CardResponse(
                entity.getCardId(), entity.getName(), entity.getSupertype(), entity.getSubtypes(),
                entity.getSetId(), entity.getSetName(), entity.getNumber(), entity.getRarity(), entity.getHp(),
                entity.getTypes(), entity.getEvolvesFrom(), entity.getRules(), entity.getAttacks(), entity.getAbilities(),
                entity.getWeaknesses(), entity.getResistances(), entity.getRetreatCost(), entity.getConvertedRetreatCost(),
                entity.getImageSmall(), entity.getImageLarge());
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String json(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("No se pudo serializar el JSON de carta", e);
        }
    }
}
