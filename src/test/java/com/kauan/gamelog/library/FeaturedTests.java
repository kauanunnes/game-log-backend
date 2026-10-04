package com.kauan.gamelog.library;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kauan.gamelog.Account;
import com.kauan.gamelog.IntegrationTest;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class FeaturedTests {
    private static final String FAVORITE = """
            {"status": "PLAYED", "favorite": true}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void featuresFavoritesInTheChosenOrderAndSwapsThem() throws Exception {
        Account ana = Account.register(mockMvc, "ft_ana");
        save(ana, "celeste", FAVORITE);
        save(ana, "hollow-knight", FAVORITE);
        save(ana, "hades", FAVORITE);

        feature(ana, "hades", "celeste")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].slug", contains("hades", "celeste")));
        mockMvc.perform(get("/api/v1/users/ft_ana"))
                .andExpect(jsonPath("$.featured[*].slug", contains("hades", "celeste")));

        feature(ana, "celeste", "hades", "hollow-knight")
                .andExpect(jsonPath("$[*].slug", contains("celeste", "hades", "hollow-knight")));
        as(ana, get("/api/v1/me/profile"))
                .andExpect(jsonPath("$.featured[*].slug", contains("celeste", "hades", "hollow-knight")));
    }

    @Test
    void onlyFavoritesUpToFiveAndEachOnce() throws Exception {
        Account bia = Account.register(mockMvc, "ft_bia");
        save(bia, "celeste", FAVORITE);
        save(bia, "portal-2", """
                {"status": "PLAYED"}""");
        feature(bia, "celeste").andExpect(status().isOk());

        feature(bia, "portal-2")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("NOT_A_FAVORITE"))
                .andExpect(jsonPath("$.errors[0].field").value("gameIds[0]"));
        feature(bia, "celeste", "celeste")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("DUPLICATE_GAME"));
        as(
                        bia,
                        put("/api/v1/me/library/featured")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {"gameIds": [1, 2, 3, 4, 5, 6]}"""))
                .andExpect(status().isUnprocessableContent());

        mockMvc.perform(get("/api/v1/users/ft_bia")).andExpect(jsonPath("$.featured[*].slug", contains("celeste")));
    }

    @Test
    void unfavoritingTakesTheGameOutOfTheSpotlight() throws Exception {
        Account caio = Account.register(mockMvc, "ft_caio");
        save(caio, "celeste", FAVORITE);
        feature(caio, "celeste").andExpect(status().isOk());

        as(
                        caio,
                        patch("/api/v1/me/library/{gameId}", game("celeste"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {"favorite": false}"""))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/ft_caio")).andExpect(jsonPath("$.featured", empty()));
    }

    @Test
    void aPrivateHeaderHidesTheSpotlight() throws Exception {
        Account davi = Account.register(mockMvc, "ft_davi");
        save(davi, "celeste", FAVORITE);
        feature(davi, "celeste").andExpect(status().isOk());
        as(
                        davi,
                        patch("/api/v1/me/settings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {"profileVisibility": "PRIVATE"}"""))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/ft_davi"))
                .andExpect(jsonPath("$.featured").doesNotExist());
        as(davi, get("/api/v1/me/profile")).andExpect(jsonPath("$.featured[*].slug", contains("celeste")));
    }

    private ResultActions feature(Account account, String... slugs) throws Exception {
        String ids =
                Arrays.stream(slugs).map(slug -> String.valueOf(game(slug))).collect(Collectors.joining(","));
        return as(
                account,
                put("/api/v1/me/library/featured")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameIds\": [" + ids + "]}"));
    }

    private void save(Account account, String slug, String body) throws Exception {
        as(
                        account,
                        put("/api/v1/me/library/{gameId}", game(slug))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().is2xxSuccessful());
    }

    private long game(String slug) {
        return jdbc.sql("SELECT id FROM games WHERE slug = :slug")
                .param("slug", slug)
                .query(Long.class)
                .single();
    }

    private ResultActions as(Account account, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, account.bearer()));
    }
}
