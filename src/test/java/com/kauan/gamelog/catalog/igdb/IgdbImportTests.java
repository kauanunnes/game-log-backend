package com.kauan.gamelog.catalog.igdb;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.kauan.gamelog.IntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class IgdbImportTests {
    private static final String OUTER_WILDS = """
            [{"id": 11737, "name": "Outer Wilds", "slug": "outer-wilds",
              "summary": "Explore a solar system stuck in a time loop.",
              "first_release_date": 1559001600, "game_type": 0, "cover": {"id": 1, "image_id": "co2e1g"},
              "genres": [{"id": 31, "name": "Adventure", "slug": "adventure"}, {"id": 32, "name": "Indie", "slug": "indie"}],
              "platforms": [{"id": 6, "name": "PC (Microsoft Windows)", "abbreviation": "PC", "slug": "win"}],
              "themes": [{"id": 18, "name": "Science fiction"}],
              "involved_companies": [
                {"id": 1, "company": {"id": 10, "name": "Mobius Digital"}, "developer": true, "publisher": false},
                {"id": 2, "company": {"id": 11, "name": "Annapurna Interactive"}, "developer": false, "publisher": true}],
              "total_rating": 88.5, "total_rating_count": 900, "similar_games": [101, 102]}]
            """;

    private static final String POPULAR = """
            [{"id": 72, "name": "Portal", "slug": "portal", "first_release_date": 1191888000, "game_type": 0,
              "genres": [{"id": 9, "name": "Puzzle", "slug": "puzzle"}], "total_rating_count": 2500},
             {"id": 99999, "name": "Some DLC", "slug": "some-dlc", "game_type": 1}]
            """;

    private static final String WITCHER = """
            [{"id": 1942, "name": "The Witcher 3: Wild Hunt", "slug": "the-witcher-3-wild-hunt",
              "first_release_date": 1431993600, "game_type": 0,
              "genres": [{"id": 12, "name": "Role-playing (RPG)", "slug": "role-playing-rpg"}]}]
            """;

    @RegisterExtension
    static WireMockExtension igdb = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .resetOnEachTest(false)
            .build();

    @DynamicPropertySource
    static void igdbProperties(DynamicPropertyRegistry registry) {
        registry.add("game-log.igdb.client-id", () -> "client-id");
        registry.add("game-log.igdb.client-secret", () -> "client-secret");
        registry.add("game-log.igdb.base-url", igdb::baseUrl);
        registry.add("game-log.igdb.token-url", () -> igdb.baseUrl() + "/oauth2/token");
        registry.add("game-log.igdb.bootstrap-limit", () -> "2");
    }

    @BeforeAll
    static void stubIgdb() {
        igdb.stubFor(post("/oauth2/token")
                .willReturn(okJson("{\"access_token\": \"token\", \"expires_in\": 3600, \"token_type\": \"bearer\"}")));
        igdb.stubFor(post("/games")
                .withRequestBody(containing("search \"outer wilds\""))
                .willReturn(okJson(OUTER_WILDS)));
        igdb.stubFor(post("/games")
                .withRequestBody(containing("sort total_rating_count desc"))
                .willReturn(okJson(POPULAR)));
        igdb.stubFor(
                post("/games").withRequestBody(containing("search \"zzz\"")).willReturn(serviceUnavailable()));
        igdb.stubFor(post("/games")
                .withRequestBody(containing("search \"qqq\""))
                .willReturn(okJson("{\"message\": \"formato inesperado\"}")));
        igdb.stubFor(
                post("/games").withRequestBody(containing("where id = 1942;")).willReturn(okJson(WITCHER)));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private IgdbCatalogSync catalogSync;

    @Test
    void bootstrapImportsPopularGamesAndSkipsUnsupportedTypes() throws Exception {
        mockMvc.perform(get("/api/v1/games/portal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.releaseDate").value("2007-10-09"));
        mockMvc.perform(get("/api/v1/games/some-dlc")).andExpect(status().isNotFound());
    }

    @Test
    void searchFallsBackToIgdbOnlyOnce() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/api/v1/games").param("q", "Outer Wilds"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Outer Wilds"))
                    .andExpect(jsonPath("$.content[0].releaseYear").value(2019))
                    .andExpect(jsonPath("$.content[0].coverUrl")
                            .value("https://images.igdb.com/igdb/image/upload/t_cover_big/co2e1g.jpg"));
        }
        igdb.verify(1, postRequestedFor(urlEqualTo("/games")).withRequestBody(containing("search \"outer wilds\"")));
    }

    @Test
    void storesGenresPlatformsAndMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/games").param("q", "outer wilds")).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/games/outer-wilds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres[*].name", contains("Adventure", "Indie")))
                .andExpect(jsonPath("$.platforms[0].abbreviation").value("PC"))
                .andExpect(jsonPath("$.developers", contains("Mobius Digital")))
                .andExpect(jsonPath("$.publishers", contains("Annapurna Interactive")))
                .andExpect(jsonPath("$.themes", contains("Science fiction")))
                .andExpect(jsonPath("$.igdbRating").value(89))
                .andExpect(jsonPath("$.igdbRatingCount").value(900));

        assertThat(jdbc.sql("SELECT count(*) FROM genres WHERE slug = 'adventure'")
                        .query(Long.class)
                        .single())
                .as("o gênero do seed foi adotado, não duplicado")
                .isEqualTo(1);
    }

    /** zzz: o IGDB responde 503 em todas as tentativas; qqq: devolve um JSON fora do formato. */
    @ParameterizedTest
    @ValueSource(strings = {"zzz", "qqq"})
    void keepsTheLocalResultsWhenIgdbFails(String query) throws Exception {
        mockMvc.perform(get("/api/v1/games").param("q", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void importsByIdAdoptingTheSeedRow() {
        long seedId = jdbc.sql("SELECT id FROM games WHERE slug = 'the-witcher-3-wild-hunt'")
                .query(Long.class)
                .single();

        assertThat(catalogSync.importById(1942)).contains(seedId);
        assertThat(jdbc.sql("SELECT igdb_id FROM games WHERE id = :id")
                        .param("id", seedId)
                        .query(Long.class)
                        .single())
                .isEqualTo(1942);
    }
}
