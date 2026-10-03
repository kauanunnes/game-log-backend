package com.kauan.gamelog.catalog.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Jogo como a API do IGDB devolve, só com os campos pedidos em {@link IgdbClient#FIELDS}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbGame(
        long id,
        String name,
        String slug,
        String summary,
        @JsonProperty("first_release_date") Long firstReleaseDate,
        @JsonProperty("game_type") Integer gameType,
        Cover cover,
        List<Named> genres,
        List<Platform> platforms,
        List<Named> themes,
        List<Named> keywords,
        @JsonProperty("game_modes") List<Named> gameModes,
        @JsonProperty("player_perspectives") List<Named> playerPerspectives,
        List<Named> franchises,
        @JsonProperty("involved_companies") List<InvolvedCompany> involvedCompanies,
        @JsonProperty("total_rating") Double totalRating,
        @JsonProperty("total_rating_count") Integer totalRatingCount,
        @JsonProperty("similar_games") List<Long> similarGames) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cover(@JsonProperty("image_id") String imageId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Named(long id, String name, String slug) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Platform(long id, String name, String abbreviation, String slug) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InvolvedCompany(Named company, boolean developer, boolean publisher) {}
}
