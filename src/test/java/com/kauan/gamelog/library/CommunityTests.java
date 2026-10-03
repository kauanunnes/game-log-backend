package com.kauan.gamelog.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kauan.gamelog.Account;
import com.kauan.gamelog.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Usa jogos que nenhum outro teste toca, porque o banco é dividido entre as classes de teste. */
@IntegrationTest
class CommunityTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void gamePageShowsTheNumbersOfPublicProfilesOnly() throws Exception {
        add(Account.register(mockMvc, "cm_ana"), "disco-elysium", """
                {"status": "PLAYED", "review": {"rating": 4.75, "recommends": true, "text": "Lindo."}}""");
        add(Account.register(mockMvc, "cm_bia"), "disco-elysium", """
                {"status": "DROPPED", "review": {"rating": 4, "recommends": false, "text": "Longo demais."}}""");
        add(Account.register(mockMvc, "cm_caio"), "disco-elysium", """
                {"status": "BACKLOG"}""");
        add(Account.register(mockMvc, "cm_davi"), "disco-elysium", """
                {"status": "WISHLIST"}""");
        Account hidden = Account.register(mockMvc, "cm_eva");
        add(hidden, "disco-elysium", """
                {"status": "PLAYED", "review": {"rating": 0.5, "recommends": false, "text": "Não gostei."}}""");
        send(patch("/api/v1/me/settings"), hidden, """
                {"profileVisibility": "PRIVATE"}""");

        mockMvc.perform(get("/api/v1/games/disco-elysium"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.community.ratingsCount").value(2))
                .andExpect(jsonPath("$.community.averageRating").value(4.38))
                .andExpect(jsonPath("$.community.ratingDistribution.length()").value(11))
                .andExpect(jsonPath("$.community.ratingDistribution[8].count").value(1))
                .andExpect(jsonPath("$.community.ratingDistribution[9].count").value(1))
                .andExpect(jsonPath("$.community.ratingDistribution[1].count").value(0))
                .andExpect(jsonPath("$.community.recommendPercent").value(50))
                .andExpect(jsonPath("$.community.playersCount").value(2))
                .andExpect(jsonPath("$.community.wantToPlayCount").value(2));

        mockMvc.perform(get("/api/v1/games/disco-elysium/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].user.username", contains("cm_bia", "cm_ana")))
                .andExpect(jsonPath("$.content[0].text").value("Longo demais."))
                .andExpect(jsonPath("$.content[0].game.slug").value("disco-elysium"));
        mockMvc.perform(get("/api/v1/reviews"))
                .andExpect(jsonPath("$.content[0].user.username").value("cm_bia"))
                .andExpect(jsonPath("$.content[*].user.username", not(hasItem("cm_eva"))));
    }

    @Test
    void aGameWithoutEntriesHasEmptyNumbers() throws Exception {
        mockMvc.perform(get("/api/v1/games/super-mario-world"))
                .andExpect(jsonPath("$.community.ratingsCount").value(0))
                .andExpect(jsonPath("$.community.averageRating").doesNotExist())
                .andExpect(jsonPath("$.community.recommendPercent").doesNotExist())
                .andExpect(jsonPath("$.community.playersCount").value(0));
        mockMvc.perform(get("/api/v1/games/nao-existe/reviews")).andExpect(status().isNotFound());
    }

    @Test
    void trendingPutsTheMostAddedOfTheWeekFirst() throws Exception {
        add(Account.register(mockMvc, "cm_fabi"), "chrono-trigger", """
                {"status": "PLAYING"}""");
        add(Account.register(mockMvc, "cm_gil"), "chrono-trigger", """
                {"status": "BACKLOG"}""");

        String page = mockMvc.perform(
                        get("/api/v1/games").param("sort", "trending").param("size", "50"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> titles = JsonPath.read(page, "$.content[*].title");

        assertThat(titles.indexOf("Chrono Trigger")).isLessThan(titles.indexOf("Super Mario World"));
    }

    private void add(Account account, String slug, String body) throws Exception {
        long gameId = jdbc.sql("SELECT id FROM games WHERE slug = :slug")
                .param("slug", slug)
                .query(Long.class)
                .single();
        send(put("/api/v1/me/library/{gameId}", gameId), account, body);
    }

    private void send(MockHttpServletRequestBuilder request, Account account, String body) throws Exception {
        mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
    }
}
