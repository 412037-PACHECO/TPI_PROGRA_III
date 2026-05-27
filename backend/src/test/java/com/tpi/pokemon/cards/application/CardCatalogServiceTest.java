package com.tpi.pokemon.cards.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpi.pokemon.cards.domain.CardRepository;
import com.tpi.pokemon.cards.infrastructure.PokemonTcgApiClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.mock.mockito.MockBean;

@DataJpaTest
@Import({CardCatalogService.class, CardMapper.class, CardCatalogServiceTest.ObjectMapperTestConfig.class})
class CardCatalogServiceTest {

    @Autowired
    private CardCatalogService service;

    @Autowired
    private CardRepository repository;

    @MockBean
    private PokemonTcgApiClient apiClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void importsCardsAndUpdatesExistingCardsOnSecondImport() throws Exception {
        when(apiClient.fetchCardsBySet("xy1")).thenReturn(List.of(card("xy1-1", "Venusaur-EX")));

        CardImportSummary firstImport = service.importXy1();
        CardImportSummary secondImport = service.importXy1();

        assertThat(firstImport).isEqualTo(new CardImportSummary(1, 1, 0, 0, 0));
        assertThat(secondImport).isEqualTo(new CardImportSummary(1, 0, 1, 0, 0));
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findByCardId("xy1-1")).isPresent();
    }

    @Test
    void skipsCardsWithoutOfficialId() throws Exception {
        when(apiClient.fetchCardsBySet("xy1")).thenReturn(List.of(objectMapper.readTree("{\"name\":\"Missing Id\"}")));

        CardImportSummary summary = service.importXy1();

        assertThat(summary).isEqualTo(new CardImportSummary(1, 0, 0, 1, 0));
        assertThat(repository.count()).isZero();
    }

    private JsonNode card(String id, String name) throws Exception {
        return objectMapper.readTree("""
                {
                  "id":"%s",
                  "name":"%s",
                  "supertype":"Pokémon",
                  "set":{"id":"xy1", "name":"XY"},
                  "number":"1"
                }
                """.formatted(id, name));
    }

    @TestConfiguration
    static class ObjectMapperTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
