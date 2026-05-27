package com.tpi.pokemon.cards.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public interface PokemonTcgApiClient {
    List<JsonNode> fetchCardsBySet(String setId);
}
