package com.tpi.pokemon.cards.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpi.pokemon.cards.domain.CardEntity;
import org.junit.jupiter.api.Test;

class CardMapperTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CardMapper mapper = new CardMapper(objectMapper);

    @Test
    void mapsApiJsonKeepingComplexFieldsAsJson() throws Exception {
        JsonNode json = objectMapper.readTree("""
                {
                  "id":"xy1-1",
                  "name":"Venusaur-EX",
                  "supertype":"Pokémon",
                  "subtypes":["Basic", "EX"],
                  "set":{"id":"xy1", "name":"XY"},
                  "number":"1",
                  "rarity":"Rare Holo EX",
                  "hp":"180",
                  "types":["Grass"],
                  "evolvesFrom":"Ivysaur",
                  "rules":["Pokémon-EX rule text"],
                  "attacks":[{"name":"Poison Powder", "damage":"60"}],
                  "weaknesses":[{"type":"Fire", "value":"×2"}],
                  "retreatCost":["Colorless", "Colorless"],
                  "convertedRetreatCost":2,
                  "images":{"small":"small.png", "large":"large.png"}
                }
                """);

        CardEntity entity = mapper.fromApiJson(json);

        assertThat(entity.getCardId()).isEqualTo("xy1-1");
        assertThat(entity.getSetId()).isEqualTo("xy1");
        assertThat(entity.getSubtypes()).isEqualTo("[\"Basic\",\"EX\"]");
        assertThat(entity.getAttacks()).contains("Poison Powder");
        assertThat(entity.getConvertedRetreatCost()).isEqualTo(2);
        assertThat(entity.getRawJson()).contains("Venusaur-EX");
    }
}
