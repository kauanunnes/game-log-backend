package com.kauan.gamelog.catalog.dto;

import com.kauan.gamelog.catalog.IgdbImages;
import java.time.LocalDate;

public record GameSummaryDTO(Long id, String slug, String title, String coverUrl, Integer releaseYear) {

    /** A partir das colunas de {@code games}, para as consultas em SQL. */
    public static GameSummaryDTO of(long id, String slug, String title, String coverImageId, LocalDate releaseDate) {
        return new GameSummaryDTO(
                id, slug, title, IgdbImages.cover(coverImageId), releaseDate == null ? null : releaseDate.getYear());
    }
}
