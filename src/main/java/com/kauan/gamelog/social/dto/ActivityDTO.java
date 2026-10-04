package com.kauan.gamelog.social.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.library.EntryStatus;
import com.kauan.gamelog.library.dto.ReviewDTO;
import com.kauan.gamelog.social.ActivityType;
import java.time.Instant;

/**
 * Um item do feed. Nunca traz loja nem valor pago.
 *
 * @param status em {@code STATUS}, o status daquele momento; nos outros tipos, o atual
 * @param completed só em {@code STATUS}
 * @param review só em {@code REVIEW}, como a avaliação está agora
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ActivityDTO(
        long id,
        ActivityType type,
        Author user,
        GameSummaryDTO game,
        EntryStatus status,
        Boolean completed,
        ReviewDTO review,
        Instant createdAt) {

    public record Author(String username, String displayName) {}
}
