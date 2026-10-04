package com.kauan.gamelog.lists.dto;

import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.lists.ListVisibility;
import java.time.Instant;
import java.util.List;

public record ListDTO(
        long id,
        String title,
        String description,
        ListVisibility visibility,
        Owner owner,
        List<Item> items,
        Instant createdAt,
        Instant updatedAt) {

    public record Owner(String username, String displayName) {}

    public record Item(int position, GameSummaryDTO game, String note) {}
}
