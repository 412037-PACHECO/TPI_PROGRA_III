package com.tpi.pokemon.game.persistence.infrastructure;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSessionRepository extends JpaRepository<GameSessionEntity, Long> {
    Optional<GameSessionEntity> findByGameId(String gameId);
}
