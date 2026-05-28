package com.tpi.pokemon.decks.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeckRepository extends JpaRepository<DeckEntity, Long> {
    @EntityGraph(attributePaths = "cards")
    Optional<DeckEntity> findWithCardsById(Long id);

    @Query("select distinct d from DeckEntity d left join fetch d.cards where lower(d.ownerName) = lower(:ownerName) order by d.updatedAt desc")
    List<DeckEntity> findByOwnerNameIgnoreCaseOrderByUpdatedAtDesc(@Param("ownerName") String ownerName);
}
