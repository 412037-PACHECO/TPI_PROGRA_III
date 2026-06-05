package com.tpi.pokemon.game.application;

import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.persistence.infrastructure.GameSessionEntity;
import com.tpi.pokemon.game.persistence.infrastructure.GameSessionRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameApplicationService {
    private final GameSessionRepository sessionRepository;
    private final GamePersistenceService persistenceService;
    private final GameQueryService queryService;

    public GameApplicationService(GameSessionRepository sessionRepository, GamePersistenceService persistenceService, GameQueryService queryService) {
        this.sessionRepository = sessionRepository;
        this.persistenceService = persistenceService;
        this.queryService = queryService;
    }

    @Transactional
    public GameSessionSummary createWaitingGame(String playerOneId) {
        String normalizedPlayerOneId = requirePlayerId(playerOneId, "playerOneId");
        String gameId = UUID.randomUUID().toString();
        GameSessionEntity session = GameSessionEntity.waiting(gameId, normalizedPlayerOneId);
        return toSummary(sessionRepository.save(session));
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
        return queryService.get(gameId);
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
