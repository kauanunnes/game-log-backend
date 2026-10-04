package com.kauan.gamelog.library.dto;

import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.library.EntryStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Avaliação pública (RF24): só com texto e, nas listas do site, só de perfis públicos. Quem decide esconder
 * spoilers é o front.
 *
 * @param id da entrada da biblioteca; é por ele que se curte (RF52)
 */
public record PublicReviewDTO(
        long id,
        Reviewer user,
        GameSummaryDTO game,
        EntryStatus status,
        BigDecimal rating,
        Boolean recommends,
        String text,
        boolean hasSpoilers,
        Instant reviewedAt,
        long likes) {

    public record Reviewer(String username, String displayName) {}
}
