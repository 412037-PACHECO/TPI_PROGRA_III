package com.tpi.pokemon.cards.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record PokemonTcgCardsResponse(List<JsonNode> data, int page, int pageSize, int count, int totalCount) {
}
