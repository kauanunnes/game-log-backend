package com.kauan.gamelog.social.dto;

import java.time.Instant;

/** Uma pessoa da lista de seguidores ou de seguidos, com a data em que a relação começou. */
public record FollowDTO(String username, String displayName, Instant followedAt) {}
