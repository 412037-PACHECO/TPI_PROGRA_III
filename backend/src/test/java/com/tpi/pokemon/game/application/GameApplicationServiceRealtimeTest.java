package com.tpi.pokemon.game.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpi.pokemon.game.application.view.GameLogPublicView;
import com.tpi.pokemon.game.application.view.GameViewResponse;
import com.tpi.pokemon.game.persistence.infrastructure.GameSessionEntity;
import com.tpi.pokemon.game.persistence.infrastructure.GameSessionRepository;
import com.tpi.pokemon.game.realtime.GameRealtimePublisher;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GameApplicationServiceRealtimeTest {
    private final GameSessionRepository sessionRepository = org.mockito.Mockito.mock(GameSessionRepository.class);
    private final GamePersistenceService persistenceService = org.mockito.Mockito.mock(GamePersistenceService.class);
    private final GameQueryService queryService = org.mockito.Mockito.mock(GameQueryService.class);
    private final GameRealtimePublisher realtimePublisher = org.mockito.Mockito.mock(GameRealtimePublisher.class);
    private final GameApplicationService service = new GameApplicationService(sessionRepository, persistenceService, queryService, realtimePublisher);

    @Test
    void createWaitingGamePublishesGameCreated() {
        when(sessionRepository.save(any(GameSessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameSessionSummary created = service.createWaitingGame("player-one");

        verify(realtimePublisher).publishGameCreated(created);
    }

    @Test
    void joinGamePublishesJoinedEventAndSafeViews() {
        GameSessionEntity waiting = GameSessionEntity.waiting("game-1", "player-one");
        GameSessionSummary joined = summary();
        GameViewResponse playerOneView = org.mockito.Mockito.mock(GameViewResponse.class);
        GameViewResponse playerTwoView = org.mockito.Mockito.mock(GameViewResponse.class);
        List<GameLogPublicView> log = List.of(publicLog());
        when(sessionRepository.findByGameIdForUpdate("game-1")).thenReturn(Optional.of(waiting));
        when(queryService.get("game-1")).thenReturn(joined);
        when(queryService.view("game-1", "player-one")).thenReturn(playerOneView);
        when(queryService.view("game-1", "player-two")).thenReturn(playerTwoView);
        when(queryService.publicHistory("game-1", "player-one")).thenReturn(log);

        service.joinGame("game-1", "player-two");

        verify(persistenceService).persistActionResult(any(), any(), eq(null), eq("GAME_CREATED"));
        verify(realtimePublisher).publishPlayerJoined(joined, playerOneView, playerTwoView, log);
    }

    @Test
    void reconnectPublishesPlayerReconnectedAndReturnsSafeView() {
        GameViewResponse view = org.mockito.Mockito.mock(GameViewResponse.class);
        List<GameLogPublicView> log = List.of(publicLog());
        when(queryService.view("game-1", "player-one")).thenReturn(view);
        when(queryService.publicHistory("game-1", "player-one")).thenReturn(log);

        GameViewResponse returned = service.reconnect("game-1", "player-one");

        org.assertj.core.api.Assertions.assertThat(returned).isSameAs(view);
        verify(realtimePublisher).publishPlayerReconnected("game-1", "player-one", view, log);
    }

    private GameSessionSummary summary() {
        Instant now = Instant.parse("2026-06-05T00:00:00Z");
        return new GameSessionSummary("game-1", "player-one", "player-two", "CREATED", null, 0, "NOT_STARTED", null, now, now);
    }

    private GameLogPublicView publicLog() {
        return new GameLogPublicView(1, 0, "player-two", "GAME_JOINED", Instant.parse("2026-06-05T00:00:00Z"), "player-two resolved GAME_JOINED", List.of("GAME_JOINED"));
    }
}
