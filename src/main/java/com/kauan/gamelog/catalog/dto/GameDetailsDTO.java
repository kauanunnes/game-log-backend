package com.kauan.gamelog.catalog.dto;

import static java.util.Comparator.comparing;

import com.kauan.gamelog.catalog.Game;
import com.kauan.gamelog.catalog.GameKind;
import com.kauan.gamelog.catalog.IgdbImages;
import java.time.LocalDate;
import java.util.List;

public record GameDetailsDTO(
        Long id,
        String slug,
        String title,
        String summary,
        LocalDate releaseDate,
        GameKind kind,
        String coverUrl,
        List<GenreDTO> genres,
        List<PlatformDTO> platforms) {

    public static GameDetailsDTO from(Game game) {
        return new GameDetailsDTO(
                game.getId(),
                game.getSlug(),
                game.getTitle(),
                game.getSummary(),
                game.getReleaseDate(),
                game.getKind(),
                IgdbImages.cover(game.getCoverImageId()),
                game.getGenres().stream()
                        .map(GenreDTO::from)
                        .sorted(comparing(GenreDTO::name))
                        .toList(),
                game.getPlatforms().stream()
                        .map(PlatformDTO::from)
                        .sorted(comparing(PlatformDTO::name))
                        .toList());
    }
}
