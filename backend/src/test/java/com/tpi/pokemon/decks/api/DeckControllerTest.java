package com.tpi.pokemon.decks.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpi.pokemon.decks.application.CatalogCardNotFoundException;
import com.tpi.pokemon.decks.application.DeckInvalidOperationException;
import com.tpi.pokemon.decks.application.DeckMapper;
import com.tpi.pokemon.decks.application.DeckService;
import com.tpi.pokemon.decks.application.DeckValidator;
import com.tpi.pokemon.decks.domain.DeckEntity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DeckController.class)
class DeckControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeckService service;
    @MockBean
    private DeckMapper mapper;
    @MockBean
    private DeckValidator validator;

    @Test
    void createsDeckUsingDtos() throws Exception {
        DeckEntity deck = new DeckEntity();
        when(service.create(new CreateDeckRequest("Starter", "Ash"))).thenReturn(deck);
        when(mapper.toDetailResponse(deck)).thenReturn(new DeckDetailResponse(1L, "Starter", "Ash", 0, null, null, List.of()));

        mockMvc.perform(post("/api/decks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDeckRequest("Starter", "Ash"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Starter"))
                .andExpect(jsonPath("$.ownerName").value("Ash"));
    }

    @Test
    void listsDecksByOwner() throws Exception {
        DeckEntity deck = new DeckEntity();
        when(service.listByOwner("Ash")).thenReturn(List.of(deck));
        when(mapper.toResponse(deck)).thenReturn(new DeckResponse(1L, "Starter", "Ash", 3, null, null));

        mockMvc.perform(get("/api/decks").param("owner", "Ash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalCards").value(3));
    }

    @Test
    void addsCardAndReturnsDetail() throws Exception {
        DeckEntity deck = new DeckEntity();
        when(service.addOrUpdateCard(1L, "xy1-1", 2)).thenReturn(deck);
        when(mapper.toDetailResponse(deck)).thenReturn(new DeckDetailResponse(1L, "Starter", "Ash", 2, null, null,
                List.of(new DeckCardResponse("xy1-1", "Pikachu", "Pokémon", "[\"Basic\"]", null, 2))));

        mockMvc.perform(put("/api/decks/1/cards/xy1-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddOrUpdateDeckCardRequest(2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards[0].cardId").value("xy1-1"))
                .andExpect(jsonPath("$.cards[0].quantity").value(2));
    }

    @Test
    void returns404WhenCatalogCardDoesNotExist() throws Exception {
        when(service.addOrUpdateCard(1L, "xy1-404", 1)).thenThrow(new CatalogCardNotFoundException("xy1-404"));

        mockMvc.perform(put("/api/decks/1/cards/xy1-404")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddOrUpdateDeckCardRequest(1))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La carta xy1-404 no existe en el catálogo local"));
    }

    @Test
    void validatesDeckAndReturnsValidationResponse() throws Exception {
        DeckEntity deck = new DeckEntity();
        when(service.getDeck(1L)).thenReturn(deck);
        when(validator.validate(deck)).thenReturn(new DeckValidationResponse(false, 12,
                List.of(new DeckValidationError("DECK_SIZE_NOT_60", null, null, "El mazo debe tener exactamente 60 cartas")),
                List.of("AS TÁCTICO / ACE SPEC no se valida como regla obligatoria para mazos xy1.")));

        mockMvc.perform(get("/api/decks/1/validation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.totalCards").value(12))
                .andExpect(jsonPath("$.errors[0].code").value("DECK_SIZE_NOT_60"));
    }

    @Test
    void returns400WhenQuantityIsNull() throws Exception {
        mockMvc.perform(put("/api/decks/1/cards/xy1-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddOrUpdateDeckCardRequest(null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El campo quantity es obligatorio"));
    }

    @Test
    void returns400WhenQuantityIsNegative() throws Exception {
        when(service.addOrUpdateCard(1L, "xy1-1", -1)).thenThrow(new DeckInvalidOperationException("La cantidad no puede ser negativa"));

        mockMvc.perform(put("/api/decks/1/cards/xy1-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddOrUpdateDeckCardRequest(-1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La cantidad no puede ser negativa"));
    }
}
