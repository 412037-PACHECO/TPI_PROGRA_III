package com.tpi.pokemon.game.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.tpi.pokemon.game.application.GameSessionSummary;
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
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class GameRealtimePublisherTest {
    private final SimpMessagingTemplate messagingTemplate = org.mockito.Mockito.mock(SimpMessagingTemplate.class);
    private final GameRealtimePublisher publisher = new GameRealtimePublisher(messagingTemplate);

    @Test
    void publishesGameCreatedToPublicTopicOnly() {
        GameSessionSummary session = session();

        publisher.publishGameCreated(session);

        ArgumentCaptor<GameRealtimeEvent> event = ArgumentCaptor.forClass(GameRealtimeEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/games/game-1/events"), event.capture());
        assertThat(event.getValue().eventId()).isNotBlank();
        assertThat(event.getValue().type()).isEqualTo(GameRealtimeEventType.GAME_CREATED);
        assertThat(event.getValue().payload()).containsEntry("playerOneId", "player-one");
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void publishesDifferentSafeViewsToEachPlayerQueue() {
        GameViewResponse playerOneView = view("player-one", 2, 3);
        GameViewResponse playerTwoView = view("player-two", 3, 2);
        List<GameLogPublicView> log = List.of(publicLog());

        publisher.publishPlayerJoined(session(), playerOneView, playerTwoView, log);

        ArgumentCaptor<GameViewUpdatedEvent> firstView = ArgumentCaptor.forClass(GameViewUpdatedEvent.class);
        ArgumentCaptor<GameViewUpdatedEvent> secondView = ArgumentCaptor.forClass(GameViewUpdatedEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/queue/games/game-1/players/player-one/view"), firstView.capture());
        verify(messagingTemplate).convertAndSend(eq("/queue/games/game-1/players/player-two/view"), secondView.capture());
        assertThat(firstView.getValue().view().viewerPlayerId()).isEqualTo("player-one");
        assertThat(secondView.getValue().view().viewerPlayerId()).isEqualTo("player-two");
        assertThat(firstView.getValue().eventId()).isNotBlank();
        assertThat(secondView.getValue().eventId()).isNotBlank();
        assertThat(firstView.getValue().view().opponent().hand().cards()).isEmpty();
        assertThat(secondView.getValue().view().opponent().hand().cards()).isEmpty();
    }

    @Test
    void publishesSanitizedLogWithoutRawJsonFields() {
        GameLogPublicView log = publicLog();

        publisher.publishSafeLog("game-1", "player-one", List.of(log));

        ArgumentCaptor<GameLogUpdatedEvent> event = ArgumentCaptor.forClass(GameLogUpdatedEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/queue/games/game-1/players/player-one/log"), event.capture());
        assertThat(event.getValue().eventId()).isNotBlank();
        assertThat(event.getValue().log()).containsExactly(log);
        assertThat(event.getValue().log().get(0).summary()).doesNotContain("commandJson", "resultJson", "eventsJson", "secretCardId");
    }

    @Test
    void publishesGameplayActionEventSafeViewsLogsAndFinishedEvent() {
        GameViewResponse playerOneView = view("player-one", 2, 3);
        GameViewResponse playerTwoView = view("player-two", 3, 2);
        List<GameLogPublicView> log = List.of(publicLog());

        publisher.publishGameplayAction("game-1", GameRealtimeEventType.ATTACK_DECLARED, "player-one", "DECLARE_ATTACK", playerOneView, playerTwoView, log, true);

        ArgumentCaptor<GameRealtimeEvent> publicEvent = ArgumentCaptor.forClass(GameRealtimeEvent.class);
        verify(messagingTemplate, org.mockito.Mockito.times(3)).convertAndSend(eq("/topic/games/game-1/events"), publicEvent.capture());
        assertThat(publicEvent.getAllValues()).extracting(GameRealtimeEvent::type)
                .containsExactly(GameRealtimeEventType.ATTACK_DECLARED, GameRealtimeEventType.LOG_UPDATED, GameRealtimeEventType.GAME_FINISHED);
        verify(messagingTemplate).convertAndSend(eq("/queue/games/game-1/players/player-one/view"), org.mockito.Mockito.any(GameViewUpdatedEvent.class));
        verify(messagingTemplate).convertAndSend(eq("/queue/games/game-1/players/player-two/view"), org.mockito.Mockito.any(GameViewUpdatedEvent.class));
        verify(messagingTemplate).convertAndSend(eq("/queue/games/game-1/players/player-one/log"), org.mockito.Mockito.any(GameLogUpdatedEvent.class));
        verify(messagingTemplate).convertAndSend(eq("/queue/games/game-1/players/player-two/log"), org.mockito.Mockito.any(GameLogUpdatedEvent.class));
    }

    @Test
    void publishesSelectionRequiredWithoutCandidateIdsInPublicEvent() {
        GameViewResponse playerOneView = view("player-one", 2, 3);
        GameViewResponse playerTwoView = view("player-two", 3, 2);

        publisher.publishGameplayAction("game-1", GameRealtimeEventType.TRAINER_PLAYED, "player-one", "PLAY_TRAINER", playerOneView, playerTwoView, List.of(publicLog()), false, true);

        ArgumentCaptor<GameRealtimeEvent> publicEvent = ArgumentCaptor.forClass(GameRealtimeEvent.class);
        verify(messagingTemplate, org.mockito.Mockito.times(3)).convertAndSend(eq("/topic/games/game-1/events"), publicEvent.capture());
        assertThat(publicEvent.getAllValues()).extracting(GameRealtimeEvent::type)
                .containsExactly(GameRealtimeEventType.TRAINER_PLAYED, GameRealtimeEventType.SELECTION_REQUIRED, GameRealtimeEventType.LOG_UPDATED);
        assertThat(publicEvent.getAllValues()).allSatisfy(event -> assertThat(event.payload()).doesNotContainKey("candidateCardIds"));
    }

    private GameSessionSummary session() {
        Instant now = Instant.parse("2026-06-05T00:00:00Z");
        return new GameSessionSummary("game-1", "player-one", "player-two", "CREATED", null, 0, "NOT_STARTED", null, now, now);
    }

    private GameViewResponse view(String viewerPlayerId, int ownHand, int opponentHand) {
        return new GameViewResponse(
                "game-1",
                "ACTIVE",
                viewerPlayerId,
                new PlayerPerspectiveView(viewerPlayerId, true, new HandView(ownHand, List.of()), new DeckView(10, false, List.of()), new PrizeCardsView(6, false, List.of()), new DiscardPileView(0, List.of()), new PlayerBoardView(null, List.of()), 1),
                new OpponentPerspectiveView(viewerPlayerId.equals("player-one") ? "player-two" : "player-one", new HandView(opponentHand, List.of()), new DeckView(10, false, List.of()), new PrizeCardsView(6, false, List.of()), new DiscardPileView(0, List.of()), new OpponentBoardView(null, List.of()), 1),
                new TurnView("player-one", "player-one", 1, "MAIN", true, false, false, false, false),
                null,
                new PendingSelectionView(false, false, null, null, null, null, null, 0, 0, false, false, List.of()),
                null,
                null,
                null
        );
    }

    private GameLogPublicView publicLog() {
        return new GameLogPublicView(1, 0, "player-two", "GAME_JOINED", Instant.parse("2026-06-05T00:00:00Z"), "player-two resolved GAME_JOINED", List.of("GAME_JOINED"));
    }
}
