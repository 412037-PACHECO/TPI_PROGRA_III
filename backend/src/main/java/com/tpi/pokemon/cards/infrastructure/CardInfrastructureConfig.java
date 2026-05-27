package com.tpi.pokemon.cards.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PokemonTcgApiProperties.class)
public class CardInfrastructureConfig {
}
