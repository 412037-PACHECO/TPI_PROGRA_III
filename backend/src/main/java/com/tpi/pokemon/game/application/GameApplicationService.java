package com.tpi.pokemon.game.application;

import com.tpi.pokemon.game.application.view.GameLogPublicView;
import com.tpi.pokemon.game.application.view.GameViewResponse;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.persistence.infrastructure.GameSessionEntity;
import com.tpi.pokemon.game.persistence.infrastructure.GameSessionRepository;
import com.tpi.pokemon.game.realtime.GameRealtimePublisher;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class GameApplicationService {
    private final GameSessionRepository sessionRepository;
    private final GamePersistenceService persistenceService;
    private final GameQueryService queryService;
    private final GameRealtimePublisher realtimePublisher;

    public GameApplicationService(GameSessionRepository sessionRepository, GamePersistenceService persistenceService, GameQueryService queryService, GameRealtimePublisher realtimePublisher) {
        this.sessionRepository = sessionRepository;
        this.persistenceService = persistenceService;
        this.queryService = queryService;
        this.realtimePublisher = realtimePublisher;
    }

    @Transactional
    public GameSessionSummary createWaitingGame(String playerOneId) {
        String normalizedPlayerOneId = requirePlayerId(playerOneId, "playerOneId");
        String gameId = UUID.randomUUID().toString();
        GameSessionEntity session = GameSessionEntity.waiting(gameId, normalizedPlayerOneId);
        GameSessionSummary created = toSummary(sessionRepository.save(session));
        publishAfterCommit(() -> realtimePublisher.publishGameCreated(created));
        return created;
    }

    @Transactional
    public GameSessionSummary joinGame(String gameId, String playerTwoId) {
        String normalizedPlayerTwoId = requirePlayerId(playerTwoId, "playerTwoId");
        GameSessionEntity session = sessionRepository.findByGameIdForUpdate(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
        if (!GameSessionStatus.WAITING.equals(session.getStatus())) {
            throw new GameAlreadyFullException(gameId);
        }
        if (normalizedPlayerTwoId.equals(session.getPlayerOneId())) {
            throw new InvalidGameCommandException("A player cannot join their own game");
        }

        GameState state = GameState.created(new GameId(gameId), new PlayerId(session.getPlayerOneId()), new PlayerId(normalizedPlayerTwoId));
        persistenceService.persistActionResult(
                state,
                new GameActionLogCommand(
                        state.getGameId(),
                        new PlayerId(normalizedPlayerTwoId),
                        "GAME_JOINED",
                        Map.of("gameId", gameId, "playerTwoId", normalizedPlayerTwoId),
                        Map.of("status", state.getStatus().name()),
                        state.getEvents().stream().map(event -> Map.of("type", event.getClass().getSimpleName())).toList()
                ),
                null,
                "GAME_CREATED"
        );
        GameSessionSummary joined = queryService.get(gameId);
        GameViewResponse playerOneView = queryService.view(gameId, joined.playerOneId());
        GameViewResponse playerTwoView = queryService.view(gameId, joined.playerTwoId());
        List<GameLogPublicView> publicLog = queryService.publicHistory(gameId, joined.playerOneId());
        publishAfterCommit(() -> realtimePublisher.publishPlayerJoined(joined, playerOneView, playerTwoView, publicLog));
        return joined;
    }

    @Transactional(readOnly = true)
    public GameViewResponse reconnect(String gameId, String playerId) {
        String normalizedPlayerId = requirePlayerId(playerId, "playerId");
        GameViewResponse view = queryService.view(gameId, normalizedPlayerId);
        List<GameLogPublicView> publicLog = queryService.publicHistory(gameId, normalizedPlayerId);
        publishAfterCommit(() -> realtimePublisher.publishPlayerReconnected(gameId, normalizedPlayerId, view, publicLog));
        return view;
    }

    private void publishAfterCommit(Runnable publication) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publication.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publication.run();
            }
        });
    }

    private String requirePlayerId(String playerId, String fieldName) {
        if (playerId == null || playerId.isBlank()) {
            throw new InvalidGameCommandException(fieldName + " is required");
        }
        return playerId.trim();
    }

    private GameSessionSummary toSummary(GameSessionEntity session) {
        return new GameSessionSummary(
                session.getGameId(),
                session.getPlayerOneId(),
                session.getPlayerTwoId(),
                session.getStatus(),
                session.getCurrentPlayerId(),
                session.getTurnNumber(),
                session.getPhase(),
                session.getWinnerId(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
