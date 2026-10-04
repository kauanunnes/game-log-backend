package com.kauan.gamelog.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kauan.gamelog.Account;
import com.kauan.gamelog.IntegrationTest;
import com.kauan.gamelog.shared.Caches;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Um cenário fechado, com os números conferidos à mão: 6 jogos, 3 com nota (5, 4,75 e 3), 75 horas, compras em
 * BRL (46,99 em 2025 e 36,99 em 2026, as duas na Steam) e em USD (24,99, sem loja).
 */
@IntegrationTest
class StatsTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private CacheManager cacheManager;

    private Account account;

    @BeforeEach
    void buildTheScenario() throws Exception {
        String username = "st_" + System.nanoTime() % 1_000_000_000;
        account = Account.register(mockMvc, username);
        add("hollow-knight", """
                {"status": "PLAYED", "review": {"rating": 5},
                 "playthrough": {"platformId": %d, "hoursPlayed": 40, "finishedOn": "2025-06-10"},
                 "acquisition": {"method": "PURCHASED", "storeId": %d, "acquiredOn": "2025-05-01",
                                 "price": {"amount": "46.99", "currency": "BRL"}}}
                """.formatted(id("platforms", "switch"), id("stores", "steam")));
        add("celeste", """
                {"status": "PLAYED", "review": {"rating": 4.75},
                 "playthrough": {"platformId": %d, "hoursPlayed": 20, "finishedOn": "2026-02-01"},
                 "acquisition": {"method": "PURCHASED", "storeId": %d, "acquiredOn": "2026-01-15",
                                 "price": {"amount": "36.99", "currency": "BRL"}}}
                """.formatted(id("platforms", "win"), id("stores", "steam")));
        add("hades", """
                {"status": "DROPPED", "review": {"rating": 3},
                 "playthrough": {"platformId": %d, "hoursPlayed": 10, "finishedOn": "2026-03-01"},
                 "acquisition": {"method": "PURCHASED", "acquiredOn": "2026-01-20",
                                 "price": {"amount": "24.99", "currency": "USD"}}}
                """.formatted(id("platforms", "switch")));
        add("elden-ring", """
                {"status": "PLAYING", "playthrough": {"platformId": %d, "hoursPlayed": 5}}
                """.formatted(id("platforms", "ps5")));
        add("stardew-valley", """
                {"status": "BACKLOG", "acquisition": {"method": "GIFT"}}""");
        add("portal-2", """
                {"status": "WISHLIST"}""");
    }

    @Test
    void addsUpTheWholeLibrary() throws Exception {
        send(get("/api/v1/me/stats"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").doesNotExist())
                .andExpect(jsonPath("$.total").value(6))
                .andExpect(jsonPath("$.byStatus.PLAYED").value(2))
                .andExpect(jsonPath("$.byStatus.PLAYING").value(1))
                .andExpect(jsonPath("$.byStatus.BACKLOG").value(1))
                .andExpect(jsonPath("$.byStatus.WISHLIST").value(1))
                .andExpect(jsonPath("$.byStatus.DROPPED").value(1))
                .andExpect(jsonPath("$.finishedByYear[*].year", contains(2025, 2026)))
                .andExpect(jsonPath("$.finishedByYear[*].count", contains(1, 2)))
                .andExpect(jsonPath(
                        "$.byGenre[*].name",
                        contains(
                                "Indie",
                                "Role-playing (RPG)",
                                "Adventure",
                                "Platform",
                                "Hack and slash/Beat 'em up",
                                "Simulator")))
                .andExpect(jsonPath("$.byGenre[*].count", contains(4, 3, 2, 2, 1, 1)))
                .andExpect(jsonPath(
                        "$.byPlatform[*].name", contains("Nintendo Switch", "PC (Microsoft Windows)", "PlayStation 5")))
                .andExpect(jsonPath("$.byPlatform[*].count", contains(2, 1, 1)))
                .andExpect(jsonPath("$.ratingDistribution.length()").value(11))
                .andExpect(jsonPath("$.ratingDistribution[6].stars").value(3.0))
                .andExpect(jsonPath("$.ratingDistribution[6].count").value(1))
                .andExpect(jsonPath("$.ratingDistribution[9].stars").value(4.5))
                .andExpect(jsonPath("$.ratingDistribution[9].count").value(1))
                .andExpect(jsonPath("$.ratingDistribution[10].count").value(1))
                .andExpect(jsonPath("$.averageRating").value(4.25))
                .andExpect(jsonPath("$.hoursPlayed").value(75))
                .andExpect(jsonPath("$.spending[*].currency", contains("BRL", "USD")))
                .andExpect(jsonPath("$.spending[0].total").value("83.98"))
                .andExpect(jsonPath("$.spending[0].purchases").value(2))
                .andExpect(jsonPath("$.spending[0].byStore[0].name").value("Steam"))
                .andExpect(jsonPath("$.spending[0].byStore[0].total").value("83.98"))
                .andExpect(jsonPath("$.spending[0].byYear[*].year", contains(2025, 2026)))
                .andExpect(jsonPath("$.spending[0].byYear[*].total", contains("46.99", "36.99")))
                .andExpect(jsonPath("$.spending[1].total").value("24.99"))
                .andExpect(jsonPath("$.spending[1].byStore[0].storeId").doesNotExist());
    }

    @Test
    void theCachedStatsFollowTheLibrary() throws Exception {
        Cache cache = cacheManager.getCache(Caches.STATS);
        long userId = jdbc.sql("SELECT id FROM users WHERE username = :username")
                .param("username", account.username())
                .query(Long.class)
                .single();
        send(get("/api/v1/me/stats"), null).andExpect(jsonPath("$.total").value(6));
        assertThat(cache.get(userId)).isNotNull();

        send(delete("/api/v1/me/library/{gameId}", id("games", "portal-2")), null)
                .andExpect(status().isNoContent());
        send(patch("/api/v1/me/library/{gameId}", id("games", "elden-ring")), """
                        {"status": "PLAYED"}""")
                .andExpect(status().isOk());
        assertThat(cache.get(userId)).isNull();

        send(get("/api/v1/me/stats"), null)
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.byStatus.WISHLIST").value(0))
                .andExpect(jsonPath("$.byStatus.PLAYED").value(3));
    }

    @Test
    void aYearCountsWhatWasFinishedAndBoughtThatYear() throws Exception {
        send(get("/api/v1/me/stats").param("year", "2026"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.byStatus.PLAYED").value(1))
                .andExpect(jsonPath("$.byStatus.DROPPED").value(1))
                .andExpect(jsonPath("$.byStatus.PLAYING").value(0))
                .andExpect(jsonPath("$.byPlatform[*].name", contains("Nintendo Switch", "PC (Microsoft Windows)")))
                .andExpect(jsonPath("$.averageRating").value(3.88))
                .andExpect(jsonPath("$.hoursPlayed").value(30))
                .andExpect(jsonPath("$.spending[*].total", contains("36.99", "24.99")));
        send(get("/api/v1/me/stats").param("year", "1800"), null).andExpect(status().isUnprocessableContent());
    }

    @Test
    void publicStatsHideTheSpendingUnlessTheOwnerAllows() throws Exception {
        String path = "/api/v1/users/" + account.username() + "/stats";
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(6))
                .andExpect(jsonPath("$.spending").doesNotExist());

        send(patch("/api/v1/me/settings"), """
                {"showSpending": true}""");
        mockMvc.perform(get(path)).andExpect(jsonPath("$.spending[0].total").value("83.98"));

        send(patch("/api/v1/me/settings"), """
                {"profileVisibility": "PRIVATE"}""");
        mockMvc.perform(get(path)).andExpect(status().isForbidden());
    }

    private void add(String slug, String body) throws Exception {
        send(put("/api/v1/me/library/{gameId}", id("games", slug)), body).andExpect(status().isCreated());
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String body) throws Exception {
        request.header(HttpHeaders.AUTHORIZATION, account.bearer());
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }

    private long id(String table, String slug) {
        return jdbc.sql("SELECT id FROM " + table + " WHERE slug = :slug")
                .param("slug", slug)
                .query(Long.class)
                .single();
    }
}
