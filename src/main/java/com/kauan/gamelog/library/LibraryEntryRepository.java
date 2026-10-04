package com.kauan.gamelog.library;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface LibraryEntryRepository extends JpaRepository<LibraryEntry, Long> {
    Optional<LibraryEntry> findByUserIdAndGameId(long userId, long gameId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "UPDATE LibraryEntry e SET e.favoritePosition = NULL WHERE e.userId = :userId AND e.favoritePosition IS NOT NULL")
    void clearFeatured(long userId);

    /** @return 0 se o jogo não é um favorito da pessoa */
    @Modifying
    @Query("UPDATE LibraryEntry e SET e.favoritePosition = :position"
            + " WHERE e.userId = :userId AND e.gameId = :gameId AND e.favorite = true")
    int feature(long userId, long gameId, short position);
}
