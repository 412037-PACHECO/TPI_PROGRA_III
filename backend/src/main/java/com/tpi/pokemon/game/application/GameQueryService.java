package com.tpi.pokemon.game.application;

import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.application.view.GameLogProjectionService;
import com.tpi.pokemon.game.application.view.GameLogPublicView;
import com.tpi.pokemon.game.application.view.GameViewProjectionService;
import com.tpi.pokemon.game.application.view.GameViewResponse;
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
    private final GameViewProjectionService viewProjectionService;
    private final GameLogProjectionService logProjectionService;

    public GameQueryService(
            GameSessionRepository sessionRepository,
            GamePersistenceService persistenceService,
            GameViewProjectionService viewProjectionService,
            GameLogProjectionService logProjectionService
    ) {
        this.sessionRepository = sessionRepository;
        this.persistenceService = persistenceService;
        this.viewProjectionService = viewProjectionService;
        this.logProjectionService = logProjectionService;
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

    @Transactional(readOnly = true)
    public GameViewResponse view(String gameId, String viewerPlayerId) {
        requireSession(gameId);
        PlayerId viewer = requireViewerPlayerId(viewerPlayerId);
        GameId id = new GameId(gameId);
        return viewProjectionService.project(
                persistenceService.loadLatestGameState(id).orElseThrow(() -> new InvalidGameCommandException("Game " + gameId + " has no persisted state yet")),
                viewer,
                persistenceService.loadLatestPendingEffectSelection(id).orElse(null)
        );
    }

    @Transactional(readOnly = true)
    public List<GameLogPublicView> publicHistory(String gameId, String viewerPlayerId) {
        requireSession(gameId);
        PlayerId viewer = requireViewerPlayerId(viewerPlayerId);
        GameId id = new GameId(gameId);
        return logProjectionService.project(
                persistenceService.loadLatestGameState(id).orElseThrow(() -> new InvalidGameCommandException("Game " + gameId + " has no persisted state yet")),
                viewer,
                persistenceService.history(id)
        );
    }

    private void requireSession(String gameId) {
        if (sessionRepository.findByGameId(gameId).isEmpty()) {
            throw new GameNotFoundException(gameId);
        }
    }

    private PlayerId requireViewerPlayerId(String viewerPlayerId) {
        if (viewerPlayerId == null || viewerPlayerId.isBlank()) {
            throw new InvalidGameCommandException("viewerPlayerId is required");
        }
        return new PlayerId(viewerPlayerId.trim());
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
