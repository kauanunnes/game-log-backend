package com.kauan.gamelog.catalog.dto;

import com.kauan.gamelog.catalog.Genre;

public record GenreDTO(Long id, String name, String slug) {
    public static GenreDTO from(Genre genre) {
        return new GenreDTO(genre.getId(), genre.getName(), genre.getSlug());
    }
}
