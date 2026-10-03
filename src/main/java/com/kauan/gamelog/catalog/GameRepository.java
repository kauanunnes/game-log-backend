package com.kauan.gamelog.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
    @EntityGraph(attributePaths = {"genres", "platforms"})
    Optional<Game> findBySlug(String slug);
}
