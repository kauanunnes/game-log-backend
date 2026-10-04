package com.kauan.gamelog.social;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
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
class LikeTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void likesAreCountedOnceAndCanSortTheReviewsOfAGame() throws Exception {
        Account ana = Account.register(mockMvc, "lk_ana");
        Account bia = Account.register(mockMvc, "lk_bia");
        Account caio = Account.register(mockMvc, "lk_caio");
        review(ana, "sekiro-shadows-die-twice", "Difícil e justo.");
        review(bia, "sekiro-shadows-die-twice", "Morri mil vezes.");
        long anas = entryId("lk_ana", "sekiro-shadows-die-twice");
        long bias = entryId("lk_bia", "sekiro-shadows-die-twice");

        like(bia, anas).andExpect(status().isNoContent());
        like(caio, anas).andExpect(status().isNoContent());
        like(caio, anas).andExpect(status().isNoContent());
        like(caio, bias).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/games/sekiro-shadows-die-twice/reviews").param("sort", "likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(anas))
                .andExpect(jsonPath("$.content[0].likes").value(2))
                .andExpect(jsonPath("$.content[1].id").value(bias))
                .andExpect(jsonPath("$.content[1].likes").value(1));
    }

    @Test
    void tellsWhichOfTheShownReviewsILikedAndUnlikes() throws Exception {
        Account davi = Account.register(mockMvc, "lk_davi");
        Account eva = Account.register(mockMvc, "lk_eva");
        review(eva, "portal-2", "Genial.");
        review(eva, "the-witcher-3-wild-hunt", "Enorme.");
        long portal = entryId("lk_eva", "portal-2");
        long witcher = entryId("lk_eva", "the-witcher-3-wild-hunt");

        like(davi, portal).andExpect(status().isNoContent());
        as(davi, get("/api/v1/me/likes").param("entryIds", portal + "," + witcher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", contains((int) portal)));

        as(davi, delete("/api/v1/reviews/{entryId}/like", portal)).andExpect(status().isNoContent());
        as(davi, delete("/api/v1/reviews/{entryId}/like", portal)).andExpect(status().isNoContent());
        as(davi, get("/api/v1/me/likes").param("entryIds", String.valueOf(portal)))
                .andExpect(jsonPath("$", empty()));
        mockMvc.perform(get("/api/v1/users/lk_eva/reviews"))
                .andExpect(jsonPath("$.content[?(@.id == " + portal + ")].likes", contains(0)));
    }

    @Test
    void refusesTheOwnReviewAndReviewsThatAreNotListed() throws Exception {
        Account fabi = Account.register(mockMvc, "lk_fabi");
        Account gil = Account.register(mockMvc, "lk_gil");
        review(fabi, "stardew-valley", "Relaxante.");
        long fabis = entryId("lk_fabi", "stardew-valley");
        save(gil, "stardew-valley", """
                {"status": "PLAYED", "review": {"rating": 4}}""");

        like(fabi, fabis)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CANNOT_LIKE_OWN_REVIEW"));
        like(fabi, entryId("lk_gil", "stardew-valley")).andExpect(status().isNotFound());
        like(fabi, 0).andExpect(status().isNotFound());

        as(
                        fabi,
                        patch("/api/v1/me/settings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {"profileVisibility": "PRIVATE"}"""))
                .andExpect(status().isOk());
        like(gil, fabis).andExpect(status().isNotFound());
    }

    @Test
    void removingTheTextTakesTheLikesAway() throws Exception {
        Account hana = Account.register(mockMvc, "lk_hana");
        Account ivo = Account.register(mockMvc, "lk_ivo");
        review(hana, "chrono-trigger", "Clássico.");
        like(ivo, entryId("lk_hana", "chrono-trigger")).andExpect(status().isNoContent());

        save(hana, "chrono-trigger", """
                {"status": "PLAYED", "review": {"rating": 5}}""");
        review(hana, "chrono-trigger", "Clássico de novo.");

        mockMvc.perform(get("/api/v1/users/lk_hana/reviews"))
                .andExpect(jsonPath("$.content[0].likes").value(0));
    }

    @Test
    void theOwnerSeesTheirReviewsEvenWhenPrivate() throws Exception {
        Account jade = Account.register(mockMvc, "lk_jade");
        review(jade, "elden-ring", "Imenso.");
        as(
                        jade,
                        patch("/api/v1/me/settings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {"profileVisibility": "PRIVATE"}"""))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/lk_jade/reviews")).andExpect(status().isForbidden());
        as(jade, get("/api/v1/me/reviews"))
                .andExpect(jsonPath("$.content[0].text").value("Imenso."))
                .andExpect(jsonPath("$.content[0].game.slug").value("elden-ring"))
                .andExpect(jsonPath("$.content[0].likes").value(0));
    }

    @Test
    void likingNeedsLogin() throws Exception {
        mockMvc.perform(put("/api/v1/reviews/1/like")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me/likes").param("entryIds", "1")).andExpect(status().isUnauthorized());
    }

    private ResultActions like(Account account, long entryId) throws Exception {
        return as(account, put("/api/v1/reviews/{entryId}/like", entryId));
    }

    private void review(Account account, String slug, String text) throws Exception {
        save(account, slug, """
                {"status": "PLAYED", "review": {"rating": 4, "text": "%s"}}""".formatted(text));
    }

    private void save(Account account, String slug, String body) throws Exception {
        long gameId = jdbc.sql("SELECT id FROM games WHERE slug = :slug")
                .param("slug", slug)
                .query(Long.class)
                .single();
        as(
                        account,
                        put("/api/v1/me/library/{gameId}", gameId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().is2xxSuccessful());
    }

    private long entryId(String username, String slug) {
        return jdbc.sql("""
                        SELECT e.id FROM library_entries e
                        JOIN users u ON u.id = e.user_id JOIN games g ON g.id = e.game_id
                        WHERE u.username = :username AND g.slug = :slug
                        """)
                .param("username", username)
                .param("slug", slug)
                .query(Long.class)
                .single();
    }

    private ResultActions as(Account account, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, account.bearer()));
    }
}
