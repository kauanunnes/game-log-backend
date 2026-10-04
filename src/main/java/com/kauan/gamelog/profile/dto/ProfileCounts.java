package com.kauan.gamelog.profile.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.kauan.gamelog.library.dto.LibraryCounts;
import com.kauan.gamelog.social.dto.FollowCounts;

/** Contadores do cabeçalho, num objeto só: as abas da biblioteca, seguidores e seguidos. */
public record ProfileCounts(
        @JsonUnwrapped LibraryCounts library, @JsonUnwrapped FollowCounts follows) {}
