package com.tpi.pokemon.game.persistence.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameActionLogRepository extends JpaRepository<GameActionLogEntity, Long> {
    List<GameActionLogEntity> findByGameIdOrderBySequenceAsc(String gameId);

    @Query("select coalesce(max(log.sequence), 0) from GameActionLogEntity log where log.gameId = :gameId")
    long findLastSequenceByGameId(@Param("gameId") String gameId);
}
