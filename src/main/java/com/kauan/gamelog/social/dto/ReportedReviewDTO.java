package com.kauan.gamelog.social.dto;

import com.kauan.gamelog.library.dto.PublicReviewDTO;
import com.kauan.gamelog.social.ReportReason;
import java.time.Instant;
import java.util.List;

/** Uma avaliação com as denúncias abertas dela, para a moderação. */
public record ReportedReviewDTO(PublicReviewDTO review, List<Report> reports) {

    public record Report(long id, ReportReason reason, String details, String reporter, Instant createdAt) {}
}
