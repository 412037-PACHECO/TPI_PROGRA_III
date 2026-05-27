package com.tpi.pokemon.cards.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<CardEntity, Long> {
    Optional<CardEntity> findByCardId(String cardId);
    boolean existsByCardId(String cardId);
    Page<CardEntity> findBySetIdIgnoreCase(String setId, Pageable pageable);
    Page<CardEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<CardEntity> findBySetIdIgnoreCaseAndNameContainingIgnoreCase(String setId, String name, Pageable pageable);
}
