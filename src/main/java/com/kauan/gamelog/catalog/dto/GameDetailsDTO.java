package com.kauan.gamelog.catalog.dto;

import static java.util.Comparator.comparing;

import com.kauan.gamelog.catalog.Game;
import com.kauan.gamelog.catalog.GameKind;
import com.kauan.gamelog.catalog.GameMetadata;
import com.kauan.gamelog.catalog.IgdbImages;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record GameDetailsDTO(
        Long id,
        String slug,
        String title,
        String summary,
        LocalDate releaseDate,
        GameKind kind,
        String coverUrl,
        List<GenreDTO> genres,
        List<PlatformDTO> platforms,
        List<String> developers,
        List<String> publishers,
        List<String> franchises,
        List<String> themes,
        List<String> modes,
        List<String> perspectives,
        // de 0 a 100, arredondada
        Integer igdbRating,
        Integer igdbRatingCount,
        CommunityDTO community) {

    private static final GameMetadata EMPTY_METADATA = new GameMetadata(null, null, null, null, null, null, null, null);

    public static GameDetailsDTO from(Game game, CommunityDTO community) {
        GameMetadata metadata = Objects.requireNonNullElse(game.getMetadata(), EMPTY_METADATA);
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
                        .toList(),
                metadata.developers(),
                metadata.publishers(),
                metadata.franchises(),
                metadata.themes(),
                metadata.modes(),
                metadata.perspectives(),
                game.getIgdbRating() == null
                        ? null
                        : game.getIgdbRating().setScale(0, RoundingMode.HALF_UP).intValue(),
                game.getIgdbRatingCount(),
                community);
    }
}
