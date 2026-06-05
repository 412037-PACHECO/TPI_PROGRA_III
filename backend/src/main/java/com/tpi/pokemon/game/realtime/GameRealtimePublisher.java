package com.tpi.pokemon.game.realtime;

import com.tpi.pokemon.game.application.GameSessionSummary;
import com.tpi.pokemon.game.application.view.GameLogPublicView;
import com.tpi.pokemon.game.application.view.GameViewResponse;
import java.util.List;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class GameRealtimePublisher {
    private final SimpMessagingTemplate messagingTemplate;

    public GameRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishGameCreated(GameSessionSummary game) {
        publishPublicEvent(GameRealtimeEvent.of(
                game.gameId(),
                GameRealtimeEventType.GAME_CREATED,
                game.playerOneId(),
                "Game created and waiting for opponent",
                Map.of("status", game.status(), "playerOneId", game.playerOneId())
        ));
    }

    public void publishPlayerJoined(GameSessionSummary game, GameViewResponse playerOneView, GameViewResponse playerTwoView, List<GameLogPublicView> log) {
        publishPublicEvent(GameRealtimeEvent.of(
                game.gameId(),
                GameRealtimeEventType.PLAYER_JOINED,
                game.playerTwoId(),
                "Player joined game",
                Map.of("status", game.status(), "playerOneId", game.playerOneId(), "playerTwoId", game.playerTwoId())
        ));
        publishSafeView(playerOneView);
        publishSafeView(playerTwoView);
        publishSafeLog(game.gameId(), game.playerOneId(), log);
        publishSafeLog(game.gameId(), game.playerTwoId(), log);
        publishPublicEvent(GameRealtimeEvent.of(game.gameId(), GameRealtimeEventType.LOG_UPDATED, game.playerTwoId(), "Public log updated", Map.of("actionType", "GAME_JOINED")));
    }

    public void publishPlayerReconnected(String gameId, String playerId, GameViewResponse view, List<GameLogPublicView> log) {
        publishPublicEvent(GameRealtimeEvent.of(
                gameId,
                GameRealtimeEventType.PLAYER_RECONNECTED,
                playerId,
                "Player reconnected",
                Map.of("playerId", playerId)
        ));
        publishSafeView(view);
        publishSafeLog(gameId, playerId, log);
        publishPublicEvent(GameRealtimeEvent.of(gameId, GameRealtimeEventType.LOG_UPDATED, playerId, "Public log updated", Map.of("actionType", "PLAYER_RECONNECTED")));
    }

    public void publishSafeView(GameViewResponse view) {
        messagingTemplate.convertAndSend(playerViewDestination(view.gameId(), view.viewerPlayerId()), GameViewUpdatedEvent.from(view));
    }

    public void publishSafeLog(String gameId, String playerId, List<GameLogPublicView> log) {
        messagingTemplate.convertAndSend(playerLogDestination(gameId, playerId), new GameLogUpdatedEvent(java.util.UUID.randomUUID().toString(), gameId, playerId, java.time.Instant.now(), List.copyOf(log)));
    }

    public void publishPublicEvent(GameRealtimeEvent event) {
        messagingTemplate.convertAndSend(publicEventsDestination(event.gameId()), event);
    }

    static String publicEventsDestination(String gameId) {
        return "/topic/games/" + destinationSegment(gameId) + "/events";
    }

    static String playerViewDestination(String gameId, String playerId) {
        return "/queue/games/" + destinationSegment(gameId) + "/players/" + destinationSegment(playerId) + "/view";
    }

    static String playerLogDestination(String gameId, String playerId) {
        return "/queue/games/" + destinationSegment(gameId) + "/players/" + destinationSegment(playerId) + "/log";
    }

    private static String destinationSegment(String value) {
        return value == null ? "unknown" : value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
