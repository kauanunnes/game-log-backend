package com.kauan.gamelog.catalog.dto;

import com.kauan.gamelog.catalog.Platform;

public record PlatformDTO(Long id, String name, String abbreviation, String slug) {
    public static PlatformDTO from(Platform platform) {
        return new PlatformDTO(platform.getId(), platform.getName(), platform.getAbbreviation(), platform.getSlug());
    }
}
