package com.tpi.pokemon.game.application;

import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.persistence.infrastructure.GameActionLogEntity;
import com.tpi.pokemon.game.persistence.infrastructure.GameSessionEntity;
import com.tpi.pokemon.game.persistence.infrastructure.GameSessionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameQueryService {
    private final GameSessionRepository sessionRepository;
    private final GamePersistenceService persistenceService;

    public GameQueryService(GameSessionRepository sessionRepository, GamePersistenceService persistenceService) {
        this.sessionRepository = sessionRepository;
        this.persistenceService = persistenceService;
    }

    @Transactional(readOnly = true)
    public GameSessionSummary get(String gameId) {
        return sessionRepository.findByGameId(gameId)
                .map(this::toSummary)
                .orElseThrow(() -> new GameNotFoundException(gameId));
    }

    @Transactional(readOnly = true)
    public List<GameSessionSummary> waitingGames() {
        return sessionRepository.findByStatusOrderByCreatedAtAsc(GameSessionStatus.WAITING).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GameActionLogSummary> history(String gameId) {
        if (sessionRepository.findByGameId(gameId).isEmpty()) {
            throw new GameNotFoundException(gameId);
        }
        return persistenceService.history(new GameId(gameId)).stream()
                .map(this::toSummary)
                .toList();
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

    private GameActionLogSummary toSummary(GameActionLogEntity log) {
        return new GameActionLogSummary(
                log.getSequence(),
                log.getTurnNumber(),
                log.getPhase(),
                log.getActorPlayerId(),
                log.getActionType(),
                log.getCommandJson(),
                log.getResultJson(),
                log.getEventsJson(),
                log.getCreatedAt()
        );
    }
}
