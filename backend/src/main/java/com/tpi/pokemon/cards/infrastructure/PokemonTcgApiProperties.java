package com.tpi.pokemon.cards.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pokemon-tcg.api")
public record PokemonTcgApiProperties(String baseUrl, String apiKey) {
}
