package com.kauan.gamelog.social;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kauan.gamelog.Account;
import com.kauan.gamelog.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class FeedTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void showsWhatThePeopleIFollowDidNewestFirst() throws Exception {
        Account ana = Account.register(mockMvc, "fd_ana");
        Account bia = Account.register(mockMvc, "fd_bia");
        Account caio = Account.register(mockMvc, "fd_caio");
        follow(ana, "fd_bia");

        save(bia, "celeste", """
                {"status": "PLAYING"}""");
        save(bia, "hollow-knight", """
                {"status": "PLAYED", "playthrough": {"completed": true}}""");
        save(caio, "hades", """
                {"status": "PLAYING"}""");
        save(ana, "hades", """
                {"status": "BACKLOG"}""");

        as(ana, get("/api/v1/me/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].game.slug", contains("hollow-knight", "celeste")))
                .andExpect(jsonPath("$.content[0].type").value("STATUS"))
                .andExpect(jsonPath("$.content[0].status").value("PLAYED"))
                .andExpect(jsonPath("$.content[0].completed").value(true))
                .andExpect(jsonPath("$.content[0].user.username").value("fd_bia"))
                .andExpect(jsonPath("$.content[0].review").doesNotExist())
                .andExpect(jsonPath("$.content[1].status").value("PLAYING"));
    }

    @Test
    void aReviewTakesThePlaceOfTheStatusAndEditingItMovesItUp() throws Exception {
        Account davi = Account.register(mockMvc, "fd_davi");
        Account eva = Account.register(mockMvc, "fd_eva");
        follow(davi, "fd_eva");

        save(eva, "celeste", """
                {"status": "PLAYED", "review": {"rating": 4.5, "text": "Difícil e linda."}}""");
        save(eva, "hades", """
                {"status": "PLAYING"}""");
        save(eva, "celeste", """
                {"status": "PLAYED", "review": {"rating": 5, "text": "Obra-prima.", "hasSpoilers": true}}""");

        as(davi, get("/api/v1/me/feed"))
                .andExpect(jsonPath("$.content[*].type", contains("REVIEW", "STATUS")))
                .andExpect(jsonPath("$.content[0].review.rating").value(5))
                .andExpect(jsonPath("$.content[0].review.text").value("Obra-prima."))
                .andExpect(jsonPath("$.content[0].review.hasSpoilers").value(true))
                .andExpect(jsonPath("$.content[0].status").value("PLAYED"))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void favoritingAddsAnActivityAndUnfavoritingTakesItBack() throws Exception {
        Account fabi = Account.register(mockMvc, "fd_fabi");
        Account gil = Account.register(mockMvc, "fd_gil");
        follow(fabi, "fd_gil");
        save(gil, "celeste", """
                {"status": "PLAYING"}""");

        change(gil, "celeste", """
                {"favorite": true}""");
        as(fabi, get("/api/v1/me/feed")).andExpect(jsonPath("$.content[*].type", contains("FAVORITE", "STATUS")));

        change(gil, "celeste", """
                {"favorite": false}""");
        as(fabi, get("/api/v1/me/feed")).andExpect(jsonPath("$.content[*].type", contains("STATUS")));
    }

    @Test
    void privateProfilesAndRemovedGamesLeaveTheFeed() throws Exception {
        Account hana = Account.register(mockMvc, "fd_hana");
        Account ivo = Account.register(mockMvc, "fd_ivo");
        follow(hana, "fd_ivo");
        save(ivo, "celeste", """
                {"status": "PLAYING"}""");
        save(ivo, "hades", """
                {"status": "PLAYING"}""");

        as(ivo, delete("/api/v1/me/library/{gameId}", gameId("hades"))).andExpect(status().isNoContent());
        as(hana, get("/api/v1/me/feed")).andExpect(jsonPath("$.content[*].game.slug", contains("celeste")));

        as(
                        ivo,
                        patch("/api/v1/me/settings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {"profileVisibility": "PRIVATE"}"""))
                .andExpect(status().isOk());
        as(hana, get("/api/v1/me/feed"))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void theFeedNeedsLogin() throws Exception {
        mockMvc.perform(get("/api/v1/me/feed")).andExpect(status().isUnauthorized());
    }

    private void follow(Account account, String username) throws Exception {
        as(account, put("/api/v1/users/{username}/follow", username)).andExpect(status().isNoContent());
    }

    private void save(Account account, String slug, String body) throws Exception {
        as(
                        account,
                        put("/api/v1/me/library/{gameId}", gameId(slug))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().is2xxSuccessful());
    }

    private void change(Account account, String slug, String patch) throws Exception {
        as(
                        account,
                        patch("/api/v1/me/library/{gameId}", gameId(slug))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(patch))
                .andExpect(status().isOk());
    }

    private long gameId(String slug) {
        return jdbc.sql("SELECT id FROM games WHERE slug = :slug")
                .param("slug", slug)
                .query(Long.class)
                .single();
    }

    private ResultActions as(Account account, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, account.bearer()));
    }
}
