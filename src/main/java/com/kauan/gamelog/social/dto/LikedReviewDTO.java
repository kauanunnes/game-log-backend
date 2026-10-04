package com.kauan.gamelog.social.dto;

import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import java.time.Instant;

/** Uma avaliação que a pessoa curtiu. */
public record LikedReviewDTO(long entryId, String author, GameSummaryDTO game, Instant likedAt) {}
