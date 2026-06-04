package com.tpi.pokemon.game.persistence.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "game_snapshots",
        uniqueConstraints = @UniqueConstraint(name = "uk_game_snapshots_game_sequence", columnNames = {"game_id", "sequence_number"}),
        indexes = @Index(name = "idx_game_snapshots_game_sequence", columnList = "game_id, sequence_number")
)
public class GameSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSessionEntity session;

    @Column(name = "game_id", nullable = false, length = 80)
    private String gameId;

    @Column(name = "sequence_number", nullable = false)
    private long sequence;

    @Column(name = "snapshot_version", nullable = false)
    private int snapshotVersion;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "current_player_id", length = 80)
    private String currentPlayerId;

    @Column(name = "turn_number", nullable = false)
    private int turnNumber;

    @Column(length = 40)
    private String phase;

    @Column(nullable = false, length = 80)
    private String reason;

    @Column(name = "action_log_id")
    private Long actionLogId;

    @Column(name = "pending_effect_type", length = 80)
    private String pendingEffectType;

    @Lob
    @Column(name = "pending_effect_selection_json", columnDefinition = "text")
    private String pendingEffectSelectionJson;

    @Lob
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "text")
    private String snapshotJson;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected GameSnapshotEntity() {
    }

    public static GameSnapshotEntity create(
            GameSessionEntity session,
            String gameId,
            long sequence,
            int snapshotVersion,
            String status,
            String currentPlayerId,
            int turnNumber,
            String phase,
            String reason,
            Long actionLogId,
            String pendingEffectType,
            String pendingEffectSelectionJson,
            String snapshotJson
    ) {
        GameSnapshotEntity snapshot = new GameSnapshotEntity();
        snapshot.session = session;
        snapshot.gameId = gameId;
        snapshot.sequence = sequence;
        snapshot.snapshotVersion = snapshotVersion;
        snapshot.status = status;
        snapshot.currentPlayerId = currentPlayerId;
        snapshot.turnNumber = turnNumber;
        snapshot.phase = phase;
        snapshot.reason = reason;
        snapshot.actionLogId = actionLogId;
        snapshot.pendingEffectType = pendingEffectType;
        snapshot.pendingEffectSelectionJson = pendingEffectSelectionJson;
        snapshot.snapshotJson = snapshotJson;
        return snapshot;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public GameSessionEntity getSession() { return session; }
    public void setSession(GameSessionEntity session) { this.session = session; }
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public long getSequence() { return sequence; }
    public void setSequence(long sequence) { this.sequence = sequence; }
    public int getSnapshotVersion() { return snapshotVersion; }
    public void setSnapshotVersion(int snapshotVersion) { this.snapshotVersion = snapshotVersion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurrentPlayerId() { return currentPlayerId; }
    public void setCurrentPlayerId(String currentPlayerId) { this.currentPlayerId = currentPlayerId; }
    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getActionLogId() { return actionLogId; }
    public void setActionLogId(Long actionLogId) { this.actionLogId = actionLogId; }
    public String getPendingEffectType() { return pendingEffectType; }
    public void setPendingEffectType(String pendingEffectType) { this.pendingEffectType = pendingEffectType; }
    public String getPendingEffectSelectionJson() { return pendingEffectSelectionJson; }
    public void setPendingEffectSelectionJson(String pendingEffectSelectionJson) { this.pendingEffectSelectionJson = pendingEffectSelectionJson; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
    public Instant getCreatedAt() { return createdAt; }
}
