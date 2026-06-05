package com.tpi.pokemon.game.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpi.pokemon.game.application.GameApplicationService;
import com.tpi.pokemon.game.application.GameNotFoundException;
import com.tpi.pokemon.game.application.GameQueryService;
import com.tpi.pokemon.game.application.GameSessionStatus;
import com.tpi.pokemon.game.application.GameSessionSummary;
import com.tpi.pokemon.game.application.GameplayApplicationService;
import com.tpi.pokemon.game.application.InvalidGameCommandException;
import com.tpi.pokemon.game.application.UnauthorizedGameViewException;
import com.tpi.pokemon.game.application.view.DeckView;
import com.tpi.pokemon.game.application.view.DiscardPileView;
import com.tpi.pokemon.game.application.view.GameLogPublicView;
import com.tpi.pokemon.game.application.view.GameViewResponse;
import com.tpi.pokemon.game.application.view.HandView;
import com.tpi.pokemon.game.application.view.OpponentBoardView;
import com.tpi.pokemon.game.application.view.OpponentPerspectiveView;
import com.tpi.pokemon.game.application.view.PendingSelectionView;
import com.tpi.pokemon.game.application.view.PlayerBoardView;
import com.tpi.pokemon.game.application.view.PlayerPerspectiveView;
import com.tpi.pokemon.game.application.view.PrizeCardsView;
import com.tpi.pokemon.game.application.view.TurnView;
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

    @MockBean
    private GameplayApplicationService gameplayService;

    @Test
    void createsWaitingGame() throws Exception {
        when(applicationService.createWaitingGame("player-one")).thenReturn(summary("game-1", "player-one", null, GameSessionStatus.WAITING));

        mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGameRequest("player-one"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value("game-1"))
                .andExpect(jsonPath("$.status").value("WAITING"));
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
    void reconnectsAndReturnsSafeView() throws Exception {
        when(applicationService.reconnect("game-1", "player-one")).thenReturn(new GameViewResponse(
                "game-1",
                "ACTIVE",
                "player-one",
                new PlayerPerspectiveView("player-one", true, new HandView(1, List.of()), new DeckView(10, false, List.of()), new PrizeCardsView(6, false, List.of()), new DiscardPileView(0, List.of()), new PlayerBoardView(null, List.of()), 1),
                new OpponentPerspectiveView("player-two", new HandView(2, List.of()), new DeckView(20, false, List.of()), new PrizeCardsView(6, false, List.of()), new DiscardPileView(0, List.of()), new OpponentBoardView(null, List.of()), 1),
                new TurnView("player-one", "player-one", 1, "MAIN", true, false, false, false, false),
                null,
                new PendingSelectionView(false, false, null, null, null, null, null, 0, 0, false, false, List.of()),
                null,
                null,
                null
        ));

        mockMvc.perform(post("/api/games/game-1/reconnect").param("playerId", "player-one"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerPlayerId").value("player-one"))
                .andExpect(jsonPath("$.opponent.hand.cards").isEmpty());
    }

    @Test
    void listsWaitingGamesAndHistory() throws Exception {
        when(queryService.waitingGames()).thenReturn(List.of(summary("game-1", "player-one", null, GameSessionStatus.WAITING)));
        when(queryService.publicHistory("game-1", "player-one")).thenReturn(List.of(new GameLogPublicView(1, 0, "player-two", "GAME_JOINED", Instant.parse("2026-06-05T00:00:00Z"), "player-two resolved GAME_JOINED", List.of("GAME_JOINED"))));

        mockMvc.perform(get("/api/games/waiting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("WAITING"));
        mockMvc.perform(get("/api/games/game-1/log").param("viewerPlayerId", "player-one"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actionType").value("GAME_JOINED"))
                .andExpect(jsonPath("$[0].commandJson").doesNotExist())
                .andExpect(jsonPath("$[0].resultJson").doesNotExist())
                .andExpect(jsonPath("$[0].eventsJson").doesNotExist());
    }

    @Test
    void returnsSafeViewAndPublicLog() throws Exception {
        when(queryService.view("game-1", "player-one")).thenReturn(new GameViewResponse(
                "game-1",
                "ACTIVE",
                "player-one",
                new PlayerPerspectiveView("player-one", true, new HandView(1, List.of()), new DeckView(10, false, List.of()), new PrizeCardsView(6, false, List.of()), new DiscardPileView(0, List.of()), new PlayerBoardView(null, List.of()), 1),
                new OpponentPerspectiveView("player-two", new HandView(2, List.of()), new DeckView(20, false, List.of()), new PrizeCardsView(6, false, List.of()), new DiscardPileView(0, List.of()), new OpponentBoardView(null, List.of()), 1),
                new TurnView("player-one", "player-one", 1, "MAIN", true, false, false, false, false),
                null,
                new PendingSelectionView(false, false, null, null, null, null, null, 0, 0, false, false, List.of()),
                null,
                null,
                null
        ));
        when(queryService.publicHistory("game-1", "player-one")).thenReturn(List.of(new GameLogPublicView(1, 0, "player-one", "GAME_JOINED", Instant.parse("2026-06-05T00:00:00Z"), "player-one resolved GAME_JOINED", List.of("GAME_JOINED"))));

        mockMvc.perform(get("/api/games/game-1/view").param("viewerPlayerId", "player-one"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerPlayerId").value("player-one"))
                .andExpect(jsonPath("$.opponent.hand.count").value(2))
                .andExpect(jsonPath("$.opponent.hand.cards").isEmpty())
                .andExpect(jsonPath("$.opponent.deck.cards").isEmpty());
        mockMvc.perform(get("/api/games/game-1/log").param("viewerPlayerId", "player-one"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actionType").value("GAME_JOINED"))
                .andExpect(jsonPath("$[0].summary").value("player-one resolved GAME_JOINED"));
    }

    @Test
    void delegatesGameplayActionAndReturnsSafeView() throws Exception {
        GameViewResponse view = safeView("game-1", "player-one");
        when(gameplayService.startTurn("game-1", new StartTurnRequest("player-one"))).thenReturn(view);
        when(gameplayService.playBasic("game-1", new PlayBasicPokemonRequest("player-one", "card-1"))).thenReturn(view);
        when(gameplayService.playTrainer("game-1", new PlayTrainerRequest("player-one", "trainer-1", null))).thenReturn(view);
        when(gameplayService.resolveSelection("game-1", new ResolveSelectionRequest("player-one", null, List.of("card-1"), null))).thenReturn(view);

        mockMvc.perform(post("/api/games/game-1/actions/start-turn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartTurnRequest("player-one"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerPlayerId").value("player-one"));
        mockMvc.perform(post("/api/games/game-1/actions/play-basic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlayBasicPokemonRequest("player-one", "card-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerPlayerId").value("player-one"));
        mockMvc.perform(post("/api/games/game-1/actions/play-trainer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlayTrainerRequest("player-one", "trainer-1", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerPlayerId").value("player-one"));
        mockMvc.perform(post("/api/games/game-1/actions/resolve-selection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResolveSelectionRequest("player-one", null, List.of("card-1"), null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerPlayerId").value("player-one"));
    }

    @Test
    void mapsGameErrorsToHttpStatuses() throws Exception {
        when(queryService.get("missing-game")).thenThrow(new GameNotFoundException("missing-game"));
        when(applicationService.createWaitingGame(null)).thenThrow(new InvalidGameCommandException("playerOneId is required"));
        when(queryService.view("game-1", "intruder")).thenThrow(new UnauthorizedGameViewException("intruder", "game-1"));

        mockMvc.perform(get("/api/games/missing-game"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Game missing-game was not found"));
        mockMvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("playerOneId is required"));
        mockMvc.perform(get("/api/games/game-1/view").param("viewerPlayerId", "intruder"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Player intruder is not allowed to view game game-1"));
    }

    private GameSessionSummary summary(String gameId, String playerOneId, String playerTwoId, String status) {
        Instant now = Instant.parse("2026-06-05T00:00:00Z");
        return new GameSessionSummary(gameId, playerOneId, playerTwoId, status, null, 0, "NOT_STARTED", null, now, now);
    }

    private GameViewResponse safeView(String gameId, String viewerPlayerId) {
        return new GameViewResponse(
                gameId,
                "ACTIVE",
                viewerPlayerId,
                new PlayerPerspectiveView(viewerPlayerId, true, new HandView(1, List.of()), new DeckView(10, false, List.of()), new PrizeCardsView(6, false, List.of()), new DiscardPileView(0, List.of()), new PlayerBoardView(null, List.of()), 1),
                new OpponentPerspectiveView("player-two", new HandView(2, List.of()), new DeckView(20, false, List.of()), new PrizeCardsView(6, false, List.of()), new DiscardPileView(0, List.of()), new OpponentBoardView(null, List.of()), 1),
                new TurnView(viewerPlayerId, viewerPlayerId, 1, "MAIN", true, false, false, false, false),
                null,
                new PendingSelectionView(false, false, null, null, null, null, null, 0, 0, false, false, List.of()),
                null,
                null,
                null
        );
    }
}
