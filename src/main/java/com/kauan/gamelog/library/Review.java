package com.kauan.gamelog.library;

import com.kauan.gamelog.library.dto.ReviewDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** @param reviewedAt muda só quando o conteúdo muda; ordena as avaliações "mais recentes" */
@Embeddable
record Review(
        BigDecimal rating,
        Boolean recommends,
        @Column(name = "review_text") String text,
        Boolean hasSpoilers,
        Instant reviewedAt) {

    static Review from(ReviewDTO dto, Review previous) {
        if (dto == null) {
            return null;
        }
        boolean unchanged = previous != null
                && sameRating(previous.rating, dto.rating())
                && Objects.equals(previous.recommends, dto.recommends())
                && Objects.equals(previous.text, dto.text())
                && Objects.equals(previous.hasSpoilers, dto.hasSpoilers());
        return new Review(
                dto.rating(),
                dto.recommends(),
                dto.text(),
                dto.hasSpoilers(),
                unchanged ? previous.reviewedAt : Instant.now());
    }

    /** 4.5 e 4.50 são a mesma nota. */
    private static boolean sameRating(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }
}
