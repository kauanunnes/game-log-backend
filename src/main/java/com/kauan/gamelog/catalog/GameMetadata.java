package com.kauan.gamelog.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Objects;

/**
 * O que vem do IGDB além das colunas próprias, guardado em {@code games.metadata} (jsonb).
 *
 * @param series as séries do IGDB ("collections"), mais completas que as franquias
 * @param parentGame id no IGDB do jogo principal de uma expansão, edição ou port
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GameMetadata(
        List<String> themes,
        List<String> keywords,
        List<String> modes,
        List<String> perspectives,
        List<String> developers,
        List<String> publishers,
        List<String> franchises,
        List<Long> similarGames,
        List<String> series,
        Long parentGame) {

    /** O seed grava {@code {}}: campo ausente vira lista vazia. */
    public GameMetadata {
        themes = Objects.requireNonNullElse(themes, List.of());
        keywords = Objects.requireNonNullElse(keywords, List.of());
        modes = Objects.requireNonNullElse(modes, List.of());
        perspectives = Objects.requireNonNullElse(perspectives, List.of());
        developers = Objects.requireNonNullElse(developers, List.of());
        publishers = Objects.requireNonNullElse(publishers, List.of());
        franchises = Objects.requireNonNullElse(franchises, List.of());
        similarGames = Objects.requireNonNullElse(similarGames, List.of());
        series = Objects.requireNonNullElse(series, List.of());
    }
}
