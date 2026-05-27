package com.tpi.pokemon.cards.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpi.pokemon.cards.application.CardCatalogService;
import com.tpi.pokemon.cards.application.CardImportSummary;
import com.tpi.pokemon.cards.application.CardMapper;
import com.tpi.pokemon.cards.domain.CardEntity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CardController.class)
class CardControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardCatalogService service;

    @MockBean
    private CardMapper mapper;

    @Test
    void importsXy1ReturningSummary() throws Exception {
        when(service.importXy1()).thenReturn(new CardImportSummary(2, 2, 0, 0, 0));

        mockMvc.perform(post("/api/cards/import/xy1"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.received").value(2))
                .andExpect(jsonPath("$.created").value(2));
    }

    @Test
    void listsCardsAsDtos() throws Exception {
        CardEntity entity = new CardEntity();
        entity.setCardId("xy1-1");
        when(service.findCards(eq("xy1"), eq("venu"), any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toResponse(entity)).thenReturn(new CardResponse("xy1-1", "Venusaur-EX", "Pokémon", null, "xy1", "XY", "1", null, "180", null, null, null, null, null, null, null, null, null, null, null));

        mockMvc.perform(get("/api/cards").param("setId", "xy1").param("name", "venu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cardId").value("xy1-1"));
    }
}
