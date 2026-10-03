package com.kauan.gamelog.library.dto;

import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.library.EntryStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Avaliação pública (RF24): só de perfis públicos e só com texto. Quem decide esconder spoilers é o front. */
public record PublicReviewDTO(
        Reviewer user,
        GameSummaryDTO game,
        EntryStatus status,
        BigDecimal rating,
        Boolean recommends,
        String text,
        boolean hasSpoilers,
        Instant reviewedAt) {

    public record Reviewer(String username, String displayName) {}
}
