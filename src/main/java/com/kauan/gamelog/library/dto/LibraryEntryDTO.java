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
        Instant updatedAt) {

    /** Sem loja e valor pago, para as rotas públicas (RN10). */
    public LibraryEntryDTO withoutSpending() {
        if (acquisition == null) {
            return this;
        }
        var hidden = new AcquisitionDTO(acquisition.method(), null, null, acquisition.acquiredOn());
        return new LibraryEntryDTO(game, status, favorite, review, playthrough, hidden, createdAt, updatedAt);
    }
}
