package com.kauan.gamelog.lists.dto;

import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.lists.ListVisibility;
import java.time.Instant;
import java.util.List;

/** @param preview os quatro primeiros jogos, para a capa da lista */
public record ListSummaryDTO(
        long id,
        String title,
        String description,
        ListVisibility visibility,
        long itemCount,
        List<GameSummaryDTO> preview,
        Instant updatedAt) {}
