package com.tpi.pokemon.game.persistence.infrastructure;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameSnapshotRepository extends JpaRepository<GameSnapshotEntity, Long> {
    Optional<GameSnapshotEntity> findTopByGameIdOrderBySequenceDesc(String gameId);

    @Query("select coalesce(max(snapshot.sequence), 0) from GameSnapshotEntity snapshot where snapshot.gameId = :gameId")
    long findLastSequenceByGameId(@Param("gameId") String gameId);
}
