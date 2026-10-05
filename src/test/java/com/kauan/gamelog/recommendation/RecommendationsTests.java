package com.kauan.gamelog.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kauan.gamelog.Account;
import com.kauan.gamelog.IntegrationTest;
import com.kauan.gamelog.library.EntryStatus;
import com.kauan.gamelog.library.dto.TasteSignal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Os jogos e as contas destes testes saem do banco no fim de cada um: outros testes contam o catálogo inteiro. */
@IntegrationTest
class RecommendationsTests {
    private static final String COLMEIA = "Abelhas guerreiras defendem a colmeia dourada cheia de mel.";
    private static final String TUBAROES = "Tubaroes famintos cercam o navio no mar escuro.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private GameIndexer indexer;

    @AfterEach
    void removeTheTestData() {
        jdbc.sql("DELETE FROM users WHERE username LIKE 'rc\\_%'").update();
        jdbc.sql("DELETE FROM games WHERE slug LIKE 'rc-%'").update();
    }

    @Test
    void suggestsNeighboursOfWhatTheUserLikesAndLeavesOutWhatDoesNotFit() throws Exception {
        long favorite = game("rc-colmeia", "Colmeia Dourada", 930001, "MAIN", "2020-01-01", null, COLMEIA);
        long twin = game("rc-colmeia-prata", "Colmeia de Prata", 930002, "MAIN", "2020-01-01", null, COLMEIA);
        long expansion = game("rc-colmeia-dlc", "Colmeia: Zangoes", 930003, "EXPANSION", "2021-01-01", null, COLMEIA);
        long edition =
                game("rc-colmeia-goty", "Colmeia Dourada GOTY", 930004, "EXPANDED", "2021-01-01", 930001L, COLMEIA);
        long future = game("rc-colmeia-futura", "Colmeia do Futuro", 930005, "MAIN", "2099-01-01", null, COLMEIA);
        long hated = game("rc-tubaroes", "Mar de Tubaroes", 930006, "MAIN", "2019-01-01", null, TUBAROES);
        long hatedTwin = game("rc-tubaroes-2", "Tubaroes no Mar", 930007, "MAIN", "2020-01-01", null, TUBAROES);
        indexer.index(List.of(favorite, twin, expansion, edition, future, hated, hatedTwin));
        Account ana = Account.register(mockMvc, "rc_ana");
        save(ana, favorite, """
                {"status": "PLAYED", "favorite": true, "review": {"rating": 5}}""");
        save(ana, hated, """
                {"status": "PLAYED", "review": {"rating": 1}}""");

        String json = recommendations(ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalized").value(true))
                .andExpect(jsonPath("$.source").value("SEARCH"))
                .andExpect(jsonPath("$.curator").doesNotExist())
                .andExpect(jsonPath("$.curating").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> slugs = JsonPath.read(json, "$.suggestions[*].game.slug");
        assertThat(slugs).contains("rc-colmeia-prata");
        assertThat(slugs)
                .doesNotContain(
                        "rc-colmeia",
                        "rc-colmeia-dlc",
                        "rc-colmeia-goty",
                        "rc-colmeia-futura",
                        "rc-tubaroes",
                        "rc-tubaroes-2");
        List<String> reasons = JsonPath.read(json, "$.suggestions[?(@.game.slug == 'rc-colmeia-prata')].reason");
        assertThat(reasons).containsExactly("Parecido com Colmeia Dourada, que você favoritou.");
    }

    @Test
    void takesAtMostTwoGamesOfTheSameSeries() throws Exception {
        long seed = game("rc-serie-1", "Saga Uma", 940001, "MAIN", "2010-01-01", null, COLMEIA, "Saga");
        long second = game("rc-serie-2", "Saga Duas", 940002, "MAIN", "2011-01-01", null, COLMEIA, "Saga");
        long third = game("rc-serie-3", "Saga Tres", 940003, "MAIN", "2012-01-01", null, COLMEIA, "Saga");
        long fourth = game("rc-serie-4", "Saga Quatro", 940004, "MAIN", "2013-01-01", null, COLMEIA, "Saga");
        indexer.index(List.of(seed, second, third, fourth));
        Account bia = Account.register(mockMvc, "rc_bia");
        save(bia, seed, """
                {"status": "PLAYED", "review": {"rating": 4.5}}""");

        String json = recommendations(bia).andReturn().getResponse().getContentAsString();

        List<String> reasons = JsonPath.read(json, "$.suggestions[?(@.game.slug =~ /rc-serie-.*/)].reason");
        assertThat(reasons).hasSize(2).containsOnly("Parecido com Saga Uma, que você avaliou com 4,5 estrelas.");
    }

    @Test
    void withoutVectorsIgdbSimilarGamesStandIn() throws Exception {
        long seed =
                game("rc-igdb", "Sem Vetor", 950001, "MAIN", "2015-01-01", null, "Um jogo.", null, 950003L, 950002L);
        game("rc-igdb-b", "Vizinho B", 950002, "MAIN", "2016-01-01", null, "Outro jogo.");
        game("rc-igdb-c", "Vizinho C", 950003, "MAIN", "2017-01-01", null, "Mais um jogo.");
        Account caio = Account.register(mockMvc, "rc_caio");
        save(caio, seed, """
                {"status": "PLAYED", "favorite": true}""");

        recommendations(caio)
                .andExpect(jsonPath("$.suggestions[0].game.slug").value("rc-igdb-c"))
                .andExpect(jsonPath("$.suggestions[1].game.slug").value("rc-igdb-b"))
                .andExpect(jsonPath("$.suggestions[0].reason").value("Parecido com Sem Vetor, que você favoritou."));
    }

    @Test
    void aNewUserGetsThePopularGames() throws Exception {
        Account davi = Account.register(mockMvc, "rc_davi");

        recommendations(davi)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalized").value(false))
                .andExpect(jsonPath("$.suggestions").isNotEmpty())
                .andExpect(jsonPath("$.suggestions[*].reason", everyItem(equalTo("Entre os mais populares do IGDB."))));
    }

    @Test
    void needsLogin() throws Exception {
        mockMvc.perform(get("/api/v1/me/recommendations")).andExpect(status().isUnauthorized());
    }

    @Test
    void weighsTheTasteAsDocumented() {
        assertThat(Recommendations.weight(signal(EntryStatus.PLAYED, true, "5", true)))
                .isEqualTo(7);
        assertThat(Recommendations.weight(signal(EntryStatus.PLAYED, false, "4", null)))
                .isEqualTo(2);
        assertThat(Recommendations.weight(signal(EntryStatus.WISHLIST, false, null, null)))
                .isOne();
        assertThat(Recommendations.weight(signal(EntryStatus.DROPPED, false, null, null)))
                .isZero();
        assertThat(Recommendations.weight(signal(EntryStatus.PLAYED, true, "2", null)))
                .isZero();
        assertThat(Recommendations.disliked(signal(EntryStatus.PLAYED, false, "4", false)))
                .isTrue();
    }

    private static TasteSignal signal(EntryStatus status, boolean favorite, String rating, Boolean recommends) {
        return new TasteSignal(
                1, status, favorite, rating == null ? null : new BigDecimal(rating), recommends, null, Instant.now());
    }

    private ResultActions recommendations(Account account) throws Exception {
        return mockMvc.perform(get("/api/v1/me/recommendations").header(HttpHeaders.AUTHORIZATION, account.bearer()));
    }

    private void save(Account account, long gameId, String body) throws Exception {
        mockMvc.perform(put("/api/v1/me/library/{gameId}", gameId)
                        .header(HttpHeaders.AUTHORIZATION, account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
    }

    private long game(
            String slug,
            String title,
            long igdbId,
            String kind,
            String releaseDate,
            Long parentGame,
            String summary,
            Object... seriesThenSimilar) {
        String series =
                seriesThenSimilar.length > 0 && seriesThenSimilar[0] instanceof String name ? "\"" + name + "\"" : "";
        String similar = Arrays.stream(seriesThenSimilar)
                .filter(Long.class::isInstance)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String metadata =
                "{\"parentGame\": %s, \"series\": [%s], \"similarGames\": [%s]}".formatted(parentGame, series, similar);
        return jdbc.sql("""
                        INSERT INTO games (igdb_id, slug, title, title_normalized, summary, kind, release_date, metadata)
                        VALUES (:igdbId, :slug, :title, lower(:title), :summary, :kind, CAST(:releaseDate AS date),
                                CAST(:metadata AS jsonb))
                        RETURNING id
                        """)
                .param("igdbId", igdbId)
                .param("slug", slug)
                .param("title", title)
                .param("summary", summary)
                .param("kind", kind)
                .param("releaseDate", releaseDate)
                .param("metadata", metadata)
                .query(Long.class)
                .single();
    }
}
