package com.tpi.pokemon.game.persistence.infrastructure;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameSessionRepository extends JpaRepository<GameSessionEntity, Long> {
    Optional<GameSessionEntity> findByGameId(String gameId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from GameSessionEntity session where session.gameId = :gameId")
    Optional<GameSessionEntity> findByGameIdForUpdate(@Param("gameId") String gameId);

    List<GameSessionEntity> findByStatusOrderByCreatedAtAsc(String status);
}
