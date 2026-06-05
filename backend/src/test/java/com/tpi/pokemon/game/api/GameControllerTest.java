package com.tpi.pokemon.game.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpi.pokemon.game.application.GameActionLogSummary;
import com.tpi.pokemon.game.application.GameApplicationService;
import com.tpi.pokemon.game.application.GameNotFoundException;
import com.tpi.pokemon.game.application.GameQueryService;
import com.tpi.pokemon.game.application.GameSessionStatus;
import com.tpi.pokemon.game.application.GameSessionSummary;
import com.tpi.pokemon.game.application.InvalidGameCommandException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GameController.class)
class GameControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GameApplicationService applicationService;

    @MockBean
    private GameQueryService queryService;

    @Test
    void createsWaitingGame() throws Exception {
        when(applicationService.createWaitingGame("player-one")).thenReturn(summary("game-1", "player-one", null, GameSessionStatus.WAITING));

        mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGameRequest("player-one"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value("game-1"))
                .andExpect(jsonPath("$.status").value(GameSessionStatus.WAITING));
    }

    @Test
    void joinsGame() throws Exception {
        when(applicationService.joinGame("game-1", "player-two")).thenReturn(summary("game-1", "player-one", "player-two", "CREATED"));

        mockMvc.perform(post("/api/games/game-1/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinGameRequest("player-two"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerTwoId").value("player-two"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void listsWaitingGamesAndHistory() throws Exception {
        when(queryService.waitingGames()).thenReturn(List.of(summary("game-1", "player-one", null, GameSessionStatus.WAITING)));
        when(queryService.history("game-1")).thenReturn(List.of(new GameActionLogSummary(1, 0, "NOT_STARTED", "player-two", "GAME_JOINED", "{}", "{}", "[]", Instant.parse("2026-06-05T00:00:00Z"))));

        mockMvc.perform(get("/api/games/waiting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value(GameSessionStatus.WAITING));
        mockMvc.perform(get("/api/games/game-1/log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actionType").value("GAME_JOINED"));
    }

    @Test
    void mapsGameErrorsToHttpStatuses() throws Exception {
        when(queryService.get("missing-game")).thenThrow(new GameNotFoundException("missing-game"));
        when(applicationService.createWaitingGame(null)).thenThrow(new InvalidGameCommandException("playerOneId is required"));

        mockMvc.perform(get("/api/games/missing-game"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Game missing-game was not found"));
        mockMvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("playerOneId is required"));
    }

    private GameSessionSummary summary(String gameId, String playerOneId, String playerTwoId, String status) {
        Instant now = Instant.parse("2026-06-05T00:00:00Z");
        return new GameSessionSummary(gameId, playerOneId, playerTwoId, status, null, 0, "NOT_STARTED", null, now, now);
    }
}
