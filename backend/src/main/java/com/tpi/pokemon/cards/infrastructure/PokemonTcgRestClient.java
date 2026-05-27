package com.tpi.pokemon.cards.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class PokemonTcgRestClient implements PokemonTcgApiClient {
    private static final int PAGE_SIZE = 250;

    private final RestClient restClient;

    public PokemonTcgRestClient(PokemonTcgApiProperties properties, RestClient.Builder builder) {
        RestClient.Builder configured = builder.baseUrl(properties.baseUrl());
        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            configured.defaultHeader("X-Api-Key", properties.apiKey());
        }
        this.restClient = configured.build();
    }

    @Override
    public List<JsonNode> fetchCardsBySet(String setId) {
        List<JsonNode> allCards = new ArrayList<>();
        int page = 1;
        int totalCount = Integer.MAX_VALUE;
        try {
            while (allCards.size() < totalCount) {
                int currentPage = page;
                PokemonTcgCardsResponse response = restClient.get()
                        .uri(uriBuilder -> uriBuilder.path("/cards")
                                .queryParam("q", "set.id:" + setId)
                                .queryParam("page", currentPage)
                                .queryParam("pageSize", PAGE_SIZE)
                                .build())
                        .header(HttpHeaders.ACCEPT, "application/json")
                        .retrieve()
                        .body(PokemonTcgCardsResponse.class);

                if (response == null || response.data() == null) {
                    throw new PokemonTcgApiException("Respuesta vacía desde pokemontcg.io", null);
                }
                allCards.addAll(response.data());
                totalCount = response.totalCount();
                if (response.data().isEmpty()) {
                    break;
                }
                page++;
            }
            return allCards;
        } catch (RestClientException e) {
            throw new PokemonTcgApiException("Error al consultar pokemontcg.io para set " + setId, e);
        }
    }
}
