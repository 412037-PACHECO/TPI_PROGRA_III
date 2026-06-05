package com.tpi.pokemon.game.persistence.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "game_sessions", uniqueConstraints = @UniqueConstraint(name = "uk_game_sessions_game_id", columnNames = "game_id"))
public class GameSessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false, unique = true, length = 80, updatable = false)
    private String gameId;

    @Column(name = "player_one_id", length = 80)
    private String playerOneId;

    @Column(name = "player_two_id", length = 80)
    private String playerTwoId;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "current_player_id", length = 80)
    private String currentPlayerId;

    @Column(name = "turn_number", nullable = false)
    private int turnNumber;

    @Column(length = 40)
    private String phase;

    @Column(name = "winner_id", length = 80)
    private String winnerId;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected GameSessionEntity() {
    }

    private GameSessionEntity(String gameId) {
        this.gameId = gameId;
    }

    public static GameSessionEntity create(String gameId) {
        return new GameSessionEntity(gameId);
    }

    public static GameSessionEntity waiting(String gameId, String playerOneId) {
        GameSessionEntity session = new GameSessionEntity(gameId);
        session.playerOneId = playerOneId;
        session.status = "WAITING";
        session.turnNumber = 0;
        session.phase = "NOT_STARTED";
        return session;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getGameId() { return gameId; }
    public String getPlayerOneId() { return playerOneId; }
    public void setPlayerOneId(String playerOneId) { this.playerOneId = playerOneId; }
    public String getPlayerTwoId() { return playerTwoId; }
    public void setPlayerTwoId(String playerTwoId) { this.playerTwoId = playerTwoId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurrentPlayerId() { return currentPlayerId; }
    public void setCurrentPlayerId(String currentPlayerId) { this.currentPlayerId = currentPlayerId; }
    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
