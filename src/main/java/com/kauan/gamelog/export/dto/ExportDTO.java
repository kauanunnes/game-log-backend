package com.kauan.gamelog.export.dto;

import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.library.dto.LibraryEntryDTO;
import com.kauan.gamelog.lists.dto.ListDTO;
import com.kauan.gamelog.social.dto.FollowDTO;
import com.kauan.gamelog.social.dto.LikedReviewDTO;
import com.kauan.gamelog.user.dto.MeDTO;
import java.time.Instant;
import java.util.List;

/** Tudo o que a conta guardou (RF10), inclusive o que é privado, como loja e valor pago. */
public record ExportDTO(
        Instant exportedAt,
        MeDTO account,
        List<LibraryEntryDTO> library,
        List<GameSummaryDTO> featured,
        List<ListDTO> lists,
        List<FollowDTO> following,
        List<FollowDTO> followers,
        List<LikedReviewDTO> likedReviews) {}
