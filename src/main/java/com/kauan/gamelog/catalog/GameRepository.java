package com.kauan.gamelog.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GameRepository extends JpaRepository<Game, Long> {
    @EntityGraph(attributePaths = {"genres", "platforms"})
    Optional<Game> findBySlug(String slug);

    @EntityGraph(attributePaths = {"genres", "platforms"})
    Optional<Game> findDetailedById(Long id);

    @Query("SELECT g.id FROM Game g WHERE g.slug = :slug")
    Optional<Long> findIdBySlug(String slug);
}
