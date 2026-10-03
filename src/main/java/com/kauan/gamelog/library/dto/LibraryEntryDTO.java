package com.kauan.gamelog.library.dto;

import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.library.EntryStatus;
import java.time.Instant;

public record LibraryEntryDTO(
        GameSummaryDTO game,
        EntryStatus status,
        boolean favorite,
        ReviewDTO review,
        PlaythroughDTO playthrough,
        AcquisitionDTO acquisition,
        Instant createdAt,
        Instant updatedAt) {}
