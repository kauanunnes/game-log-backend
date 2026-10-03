package com.kauan.gamelog.catalog;

import com.kauan.gamelog.shared.TextNormalizer;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record GameSearch(
        @Size(max = 100) String q,
        Long genreId,
        Long platformId,
        @Min(1950) @Max(2100) Integer year,
        GameSort sort) {

    /** Normaliza o texto buscado e escolhe a ordenação padrão: relevância com texto, popularidade sem. */
    GameSearch normalized() {
        String query = q == null || q.isBlank() ? null : TextNormalizer.normalize(q);
        GameSort order = sort == null || sort == GameSort.RELEVANCE
                ? (query == null ? GameSort.POPULAR : GameSort.RELEVANCE)
                : sort;
        return new GameSearch(query, genreId, platformId, year, order);
    }
}
