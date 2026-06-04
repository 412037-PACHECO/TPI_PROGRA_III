package com.tpi.pokemon.game.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.engine.effect.PendingEffectSelection;
import com.tpi.pokemon.game.persistence.infrastructure.GameActionLogEntity;
import com.tpi.pokemon.game.persistence.infrastructure.GameActionLogRepository;
import com.tpi.pokemon.game.persistence.infrastructure.GameSessionEntity;
import com.tpi.pokemon.game.persistence.infrastructure.GameSessionRepository;
import com.tpi.pokemon.game.persistence.infrastructure.GameSnapshotEntity;
import com.tpi.pokemon.game.persistence.infrastructure.GameSnapshotRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GamePersistenceService {
    private final GameSessionRepository sessionRepository;
    private final GameSnapshotRepository snapshotRepository;
    private final GameActionLogRepository actionLogRepository;
    private final GameStateSnapshotMapper snapshotMapper;
    private final ObjectMapper objectMapper;

    public GamePersistenceService(
            GameSessionRepository sessionRepository,
            GameSnapshotRepository snapshotRepository,
            GameActionLogRepository actionLogRepository,
            GameStateSnapshotMapper snapshotMapper,
            ObjectMapper objectMapper
    ) {
        this.sessionRepository = sessionRepository;
        this.snapshotRepository = snapshotRepository;
        this.actionLogRepository = actionLogRepository;
        this.snapshotMapper = snapshotMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GameSnapshotEntity saveSnapshot(GameState state) {
        return saveSnapshot(state, GameSnapshotCommand.automatic("STATE_PERSISTED"));
    }

    @Transactional
    public GameSnapshotEntity saveSnapshot(GameState state, GameSnapshotCommand command) {
        String gameId = state.getGameId().value();
        GameSessionEntity session = upsertSession(state);
        long nextSequence = snapshotRepository.findLastSequenceByGameId(gameId) + 1;

        GameSnapshotEntity snapshot = GameSnapshotEntity.create(
                session,
                gameId,
                nextSequence,
                GameStateSnapshotMapper.SNAPSHOT_VERSION,
                state.getStatus().name(),
                state.getTurnState().currentPlayer() == null ? null : state.getTurnState().currentPlayer().value(),
                state.getTurnState().turnNumber(),
                state.getTurnState().phase().name(),
                command.reason(),
                command.actionLogId(),
                command.pendingEffectSelection() == null ? null : command.pendingEffectSelection().effectType().name(),
                snapshotMapper.pendingEffectSelectionToJson(command.pendingEffectSelection()),
                snapshotMapper.toJson(state, nextSequence, command.pendingEffectSelection())
        );
        return snapshotRepository.save(snapshot);
    }

    @Transactional(readOnly = true)
    public Optional<GameSnapshotEntity> findLatestSnapshot(GameId gameId) {
        return snapshotRepository.findTopByGameIdOrderBySequenceDesc(gameId.value());
    }

    @Transactional(readOnly = true)
    public Optional<GameState> loadLatestGameState(GameId gameId) {
        return findLatestSnapshot(gameId).map(snapshot -> snapshotMapper.fromJson(snapshot.getSnapshotJson()));
    }

    @Transactional(readOnly = true)
    public Optional<PendingEffectSelection> loadLatestPendingEffectSelection(GameId gameId) {
        return findLatestSnapshot(gameId)
                .flatMap(snapshot -> snapshotMapper.pendingEffectSelectionFromJson(snapshot.getPendingEffectSelectionJson()));
    }

    @Transactional(readOnly = true)
    public GameState requireLatestGameState(GameId gameId) {
        return loadLatestGameState(gameId)
                .orElseThrow(() -> new GamePersistenceException("No snapshot found for game " + gameId.value()));
    }

    @Transactional
    public GameActionLogEntity appendActionLog(GameState state, GameActionLogCommand command) {
        if (!state.getGameId().equals(command.gameId())) {
            throw new IllegalArgumentException("Command gameId must match state gameId");
        }

        String gameId = state.getGameId().value();
        GameSessionEntity session = upsertSession(state);
        long nextSequence = actionLogRepository.findLastSequenceByGameId(gameId) + 1;

        GameActionLogEntity log = GameActionLogEntity.create(
                session,
                gameId,
                nextSequence,
                state.getTurnState().turnNumber(),
                state.getTurnState().phase().name(),
                command.actorPlayerId() == null ? null : command.actorPlayerId().value(),
                command.actionType(),
                toJsonOrNull(command.commandPayload()),
                toJsonOrNull(command.resultPayload()),
                toJsonOrNull(command.eventsPayload())
        );
        return actionLogRepository.save(log);
    }

    @Transactional
    public PersistedGameAction persistActionResult(GameState state, GameActionLogCommand command) {
        return persistActionResult(state, command, null, "ACTION_RESOLVED");
    }

    @Transactional
    public PersistedGameAction persistActionResult(GameState state, GameActionLogCommand command, PendingEffectSelection pendingEffectSelection, String snapshotReason) {
        GameActionLogEntity log = appendActionLog(state, command);
        GameSnapshotEntity snapshot = saveSnapshot(state, new GameSnapshotCommand(snapshotReason, log.getId(), pendingEffectSelection));
        return new PersistedGameAction(log, snapshot);
    }

    @Transactional(readOnly = true)
    public java.util.List<GameActionLogEntity> history(GameId gameId) {
        return actionLogRepository.findByGameIdOrderBySequenceAsc(gameId.value());
    }

    public record PersistedGameAction(GameActionLogEntity actionLog, GameSnapshotEntity snapshot) {}

    private GameSessionEntity upsertSession(GameState state) {
        String gameId = state.getGameId().value();
        GameSessionEntity session = sessionRepository.findByGameId(gameId).orElseGet(() -> GameSessionEntity.create(gameId));
        session.setPlayerOneId(state.getPlayerOneState().getPlayerId().value());
        session.setPlayerTwoId(state.getPlayerTwoState().getPlayerId().value());
        session.setStatus(state.getStatus().name());
        session.setCurrentPlayerId(state.getTurnState().currentPlayer() == null ? null : state.getTurnState().currentPlayer().value());
        session.setTurnNumber(state.getTurnState().turnNumber());
        session.setPhase(state.getTurnState().phase().name());
        session.setWinnerId(state.getFinishResult().flatMap(result -> result.winner()).map(winner -> winner.value()).orElse(null));
        if (state.getStatus() == com.tpi.pokemon.game.domain.enums.GameStatus.FINISHED && session.getFinishedAt() == null) {
            session.setFinishedAt(java.time.Instant.now());
        }
        return sessionRepository.save(session);
    }

    private String toJsonOrNull(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new GamePersistenceException("Could not serialize game action log payload", e);
        }
    }
}
