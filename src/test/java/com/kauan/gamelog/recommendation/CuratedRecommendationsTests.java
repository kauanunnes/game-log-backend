package com.kauan.gamelog.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kauan.gamelog.Account;
import com.kauan.gamelog.FakeCurator;
import com.kauan.gamelog.IntegrationTest;
import com.kauan.gamelog.catalog.dto.GameProfile;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Os jogos e as contas destes testes saem do banco no fim de cada um: outros testes contam o catálogo inteiro. */
@IntegrationTest
class CuratedRecommendationsTests {
    private static final String COLMEIA = "Abelhas guerreiras defendem a colmeia dourada cheia de mel.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private GameIndexer indexer;

    @Autowired
    private FakeCurator curator;

    private long liked;
    private long candidate;

    @BeforeEach
    void buildTheCatalog() {
        liked = game("cr-colmeia", "Colmeia Dourada", 960001);
        candidate = game("cr-colmeia-prata", "Colmeia de Prata", 960002);
        indexer.index(List.of(liked, candidate));
    }

    @AfterEach
    void removeTheTestData() {
        curator.turnOff();
        jdbc.sql("DELETE FROM users WHERE username LIKE 'cr\\_%'").update();
        jdbc.sql("DELETE FROM games WHERE slug LIKE 'cr-%'").update();
    }

    @Test
    void theModelChoosesAndExplainsInTheBackground() throws Exception {
        AtomicReference<Curator.Input> asked = new AtomicReference<>();
        curator.answerWith(input -> {
            asked.set(input);
            return List.of(new Curator.Pick(candidate, "Tem as mesmas abelhas de Colmeia Dourada."));
        });
        Account ana = Account.register(mockMvc, "cr_ana");
        save(ana, liked, """
                {"status": "PLAYED", "favorite": true, "review": {"rating": 5, "text": "Amei as abelhas."}}""");

        recommendations(ana)
                .andExpect(jsonPath("$.source").value("SEARCH"))
                .andExpect(jsonPath("$.curator").value("Teste"))
                .andExpect(jsonPath("$.curating").value(true));

        await().untilAsserted(() -> recommendations(ana)
                .andExpect(jsonPath("$.source").value("AI"))
                .andExpect(jsonPath("$.curator").value("Teste"))
                .andExpect(jsonPath("$.curating").value(false))
                .andExpect(jsonPath("$.suggestions[0].game.slug").value("cr-colmeia-prata"))
                .andExpect(jsonPath("$.suggestions[0].reason").value("Tem as mesmas abelhas de Colmeia Dourada.")));
        Curator.Input input = asked.get();
        assertThat(input.liked()).singleElement().satisfies(game -> {
            assertThat(game.title()).isEqualTo("Colmeia Dourada");
            assertThat(game.review()).isEqualTo("Amei as abelhas.");
        });
        assertThat(input.candidates())
                .extracting(GameProfile::id)
                .contains(candidate)
                .doesNotContain(liked);
    }

    @Test
    void aFailureKeepsTheSearchAndWaitsBeforeTryingAgain() throws Exception {
        curator.answerWith(input -> {
            throw new IllegalStateException("fora do ar");
        });
        Account bia = Account.register(mockMvc, "cr_bia");
        save(bia, liked, """
                {"status": "PLAYED", "favorite": true}""");

        recommendations(bia).andExpect(jsonPath("$.curating").value(true));
        await().untilAsserted(() -> recommendations(bia)
                .andExpect(jsonPath("$.curating").value(false))
                .andExpect(jsonPath("$.source").value("SEARCH")));

        recommendations(bia).andExpect(jsonPath("$.curating").value(false));
        assertThat(curator.calls()).isOne();
    }

    @Test
    void aLibraryChangeAsksAgain() throws Exception {
        curator.answerWith(input -> List.of(new Curator.Pick(candidate, "Escolhido.")));
        Account caio = Account.register(mockMvc, "cr_caio");
        save(caio, liked, """
                {"status": "PLAYED", "favorite": true}""");
        recommendations(caio);
        await().untilAsserted(() ->
                recommendations(caio).andExpect(jsonPath("$.source").value("AI")));

        save(caio, liked, """
                {"status": "PLAYED", "favorite": true, "review": {"rating": 4.5}}""");

        recommendations(caio)
                .andExpect(jsonPath("$.source").value("SEARCH"))
                .andExpect(jsonPath("$.curating").value(true));
        await().untilAsserted(() -> assertThat(curator.calls()).isEqualTo(2));
    }

    private ResultActions recommendations(Account account) throws Exception {
        return mockMvc.perform(get("/api/v1/me/recommendations").header(HttpHeaders.AUTHORIZATION, account.bearer()))
                .andExpect(status().isOk());
    }

    private void save(Account account, long gameId, String body) throws Exception {
        mockMvc.perform(put("/api/v1/me/library/{gameId}", gameId)
                        .header(HttpHeaders.AUTHORIZATION, account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
    }

    private long game(String slug, String title, long igdbId) {
        return jdbc.sql("""
                        INSERT INTO games (igdb_id, slug, title, title_normalized, summary, release_date)
                        VALUES (:igdbId, :slug, :title, lower(:title), :summary, DATE '2020-01-01')
                        RETURNING id
                        """)
                .param("igdbId", igdbId)
                .param("slug", slug)
                .param("title", title)
                .param("summary", COLMEIA)
                .query(Long.class)
                .single();
    }
}
