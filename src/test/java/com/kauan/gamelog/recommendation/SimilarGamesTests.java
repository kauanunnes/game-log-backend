package com.kauan.gamelog.recommendation;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kauan.gamelog.IntegrationTest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

/** Os jogos destes testes saem do banco no fim de cada um: outros testes contam o catálogo inteiro. */
@IntegrationTest
class SimilarGamesTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private GameIndexer indexer;

    @AfterEach
    void removeTheTestGames() {
        jdbc.sql("DELETE FROM games WHERE slug LIKE 'sg-%'").update();
    }

    @Test
    void byContentIsTheNearestVectorsWithoutTheGameAndItsExpansions() throws Exception {
        long base =
                game("sg-reino", "Reino das Abelhas", 910001, null, "Abelhas guerreiras defendem a colmeia e o mel.");
        long expansion =
                game("sg-reino-rainha", "Reino das Abelhas: Rainha", 910002, 910001L, "Abelhas guerreiras e a rainha.");
        long cousin = game("sg-colmeia", "Colmeia", 910003, null, "Abelhas guerreiras defendem a colmeia e o mel.");
        indexer.index(List.of(base, expansion, cousin));

        mockMvc.perform(get("/api/v1/games/sg-reino/similar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byContent[0].slug").value("sg-colmeia"))
                .andExpect(jsonPath("$.byContent[*].slug", not(hasItem("sg-reino"))))
                .andExpect(jsonPath("$.byContent[*].slug", not(hasItem("sg-reino-rainha"))));
        mockMvc.perform(get("/api/v1/games/sg-reino-rainha/similar"))
                .andExpect(jsonPath("$.byContent[*].slug", hasItem("sg-reino")));
    }

    @Test
    void byIgdbKeepsTheIgdbOrderAndSkipsGamesOutsideTheCatalog() throws Exception {
        game("sg-a", "Jogo A", 920001, null, "Um jogo.", 920003L, 999999L, 920002L);
        game("sg-b", "Jogo B", 920002, null, "Outro jogo.");
        game("sg-c", "Jogo C", 920003, null, "Mais um jogo.");

        mockMvc.perform(get("/api/v1/games/sg-a/similar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byIgdb[*].slug", contains("sg-c", "sg-b")))
                .andExpect(jsonPath("$.byContent").value(nullValue()));
    }

    @Test
    void anUnknownGameIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/games/sg-nao-existe/similar")).andExpect(status().isNotFound());
    }

    private long game(String slug, String title, long igdbId, Long parentGame, String summary, Long... similarGames) {
        String similar = Arrays.stream(similarGames).map(String::valueOf).collect(Collectors.joining(","));
        return jdbc.sql("""
                        INSERT INTO games (igdb_id, slug, title, title_normalized, summary, metadata)
                        VALUES (:igdbId, :slug, :title, lower(:title), :summary, CAST(:metadata AS jsonb))
                        RETURNING id
                        """)
                .param("igdbId", igdbId)
                .param("slug", slug)
                .param("title", title)
                .param("summary", summary)
                .param("metadata", "{\"parentGame\": %s, \"similarGames\": [%s]}".formatted(parentGame, similar))
                .query(Long.class)
                .single();
    }
}
