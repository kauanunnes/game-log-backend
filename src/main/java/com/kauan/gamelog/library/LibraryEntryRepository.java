package com.kauan.gamelog.library;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface LibraryEntryRepository extends JpaRepository<LibraryEntry, Long> {
    Optional<LibraryEntry> findByUserIdAndGameId(long userId, long gameId);
}
