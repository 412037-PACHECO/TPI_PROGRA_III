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
        name = "game_action_logs",
        uniqueConstraints = @UniqueConstraint(name = "uk_game_action_logs_game_sequence", columnNames = {"game_id", "sequence_number"}),
        indexes = @Index(name = "idx_game_action_logs_game_sequence", columnList = "game_id, sequence_number")
)
public class GameActionLogEntity {
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

    @Column(name = "turn_number", nullable = false)
    private int turnNumber;

    @Column(length = 40)
    private String phase;

    @Column(name = "actor_player_id", length = 80)
    private String actorPlayerId;

    @Column(name = "action_type", nullable = false, length = 80)
    private String actionType;

    @Lob
    @Column(name = "command_json", nullable = false, columnDefinition = "text")
    private String commandJson;

    @Lob
    @Column(name = "result_json", nullable = false, columnDefinition = "text")
    private String resultJson;

    @Lob
    @Column(name = "events_json", columnDefinition = "text")
    private String eventsJson;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected GameActionLogEntity() {
    }

    public static GameActionLogEntity create(
            GameSessionEntity session,
            String gameId,
            long sequence,
            int turnNumber,
            String phase,
            String actorPlayerId,
            String actionType,
            String commandJson,
            String resultJson,
            String eventsJson
    ) {
        GameActionLogEntity log = new GameActionLogEntity();
        log.session = session;
        log.gameId = gameId;
        log.sequence = sequence;
        log.turnNumber = turnNumber;
        log.phase = phase;
        log.actorPlayerId = actorPlayerId;
        log.actionType = actionType;
        log.commandJson = commandJson;
        log.resultJson = resultJson;
        log.eventsJson = eventsJson;
        return log;
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
    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public String getActorPlayerId() { return actorPlayerId; }
    public void setActorPlayerId(String actorPlayerId) { this.actorPlayerId = actorPlayerId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getCommandJson() { return commandJson; }
    public void setCommandJson(String commandJson) { this.commandJson = commandJson; }
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
    public String getEventsJson() { return eventsJson; }
    public void setEventsJson(String eventsJson) { this.eventsJson = eventsJson; }
    public Instant getCreatedAt() { return createdAt; }
}
