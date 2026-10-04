package com.kauan.gamelog.profile;

import static org.hamcrest.Matchers.contains;
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
class ProfileControllerTests {
    private static final String PURCHASE = """
            {"status": "PLAYED", "favorite": true, "review": {"rating": 5, "text": "Obra-prima."},
             "acquisition": {"method": "PURCHASED", "storeId": %d, "price": {"amount": "46.99", "currency": "BRL"}}}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void showsTheHeaderWithTheCountsOfEachTab() throws Exception {
        Account account = Account.register(mockMvc, "pf_ana");
        send(patch("/api/v1/me"), account, """
                {"displayName": "Ana", "bio": "Metroidvanias e RPGs.", "gender": "FEMALE"}
                """);
        add(account, "hollow-knight", PURCHASE.formatted(store("steam")));
        add(account, "celeste", """
                {"status": "PLAYING"}""");
        add(account, "hades", """
                {"status": "WISHLIST"}""");

        mockMvc.perform(get("/api/v1/users/{username}", "PF_ANA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("pf_ana"))
                .andExpect(jsonPath("$.displayName").value("Ana"))
                .andExpect(jsonPath("$.private").value(false))
                .andExpect(jsonPath("$.bio").value("Metroidvanias e RPGs."))
                .andExpect(jsonPath("$.gender").value("FEMALE"))
                .andExpect(jsonPath("$.memberSince").isNotEmpty())
                .andExpect(jsonPath("$.counts.played").value(1))
                .andExpect(jsonPath("$.counts.playing").value(1))
                .andExpect(jsonPath("$.counts.wishlist").value(1))
                .andExpect(jsonPath("$.counts.favorites").value(1))
                .andExpect(jsonPath("$.counts.reviews").value(1));
    }

    @Test
    void neverShowsTheStoreOrThePriceUnlessTheOwnerAllows() throws Exception {
        Account account = Account.register(mockMvc, "pf_bia");
        add(account, "celeste", PURCHASE.formatted(store("steam")));

        for (String tab : new String[] {"library", "favorites", "reviews"}) {
            mockMvc.perform(get("/api/v1/users/pf_bia/" + tab))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].acquisition.method").value("PURCHASED"))
                    .andExpect(jsonPath("$.content[0].acquisition.price").doesNotExist())
                    .andExpect(jsonPath("$.content[0].acquisition.storeId").doesNotExist());
        }

        send(patch("/api/v1/me/settings"), account, """
                {"showSpending": true}
                """);
        mockMvc.perform(get("/api/v1/users/pf_bia/library"))
                .andExpect(jsonPath("$.content[0].acquisition.price.amount").value("46.99"));
    }

    @Test
    void privateProfilesShowOnlyTheHeader() throws Exception {
        Account account = Account.register(mockMvc, "pf_caio");
        send(patch("/api/v1/me"), account, """
                {"displayName": "Caio", "bio": "Não devia aparecer.", "gender": "MALE"}
                """);
        send(patch("/api/v1/me/settings"), account, """
                {"profileVisibility": "PRIVATE"}
                """);

        mockMvc.perform(get("/api/v1/users/pf_caio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("pf_caio"))
                .andExpect(jsonPath("$.displayName").value("Caio"))
                .andExpect(jsonPath("$.private").value(true))
                .andExpect(jsonPath("$.bio").doesNotExist())
                .andExpect(jsonPath("$.gender").doesNotExist())
                .andExpect(jsonPath("$.counts").doesNotExist());
        for (String tab : new String[] {"library", "favorites", "reviews"}) {
            mockMvc.perform(get("/api/v1/users/pf_caio/" + tab))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Este perfil é privado."));
        }
    }

    @Test
    void tabsFilterLikeTheOwnLibrary() throws Exception {
        Account account = Account.register(mockMvc, "pf_davi");
        add(account, "hollow-knight", """
                {"status": "PLAYED", "review": {"rating": 5}}""");
        add(account, "celeste", """
                {"status": "PLAYED", "review": {"rating": 4, "text": "Difícil e bonito."}}""");
        add(account, "hades", """
                {"status": "PLAYING"}""");

        mockMvc.perform(get("/api/v1/users/pf_davi/library")
                        .param("status", "PLAYED")
                        .param("sort", "rating,desc"))
                .andExpect(jsonPath("$.content[*].game.title", contains("Hollow Knight", "Celeste")));
        mockMvc.perform(get("/api/v1/users/pf_davi/reviews"))
                .andExpect(jsonPath("$.content[*].game.title", contains("Celeste")));
    }

    @Test
    void theOwnerSeesTheWholeHeaderEvenWhenPrivate() throws Exception {
        Account account = Account.register(mockMvc, "pf_lia");
        send(patch("/api/v1/me"), account, """
                {"displayName": "Lia", "bio": "Só eu vejo."}
                """);
        send(patch("/api/v1/me/settings"), account, """
                {"profileVisibility": "PRIVATE"}
                """);
        add(account, "celeste", """
                {"status": "PLAYING"}""");

        mockMvc.perform(get("/api/v1/me/profile").header(HttpHeaders.AUTHORIZATION, account.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.private").value(true))
                .andExpect(jsonPath("$.bio").value("Só eu vejo."))
                .andExpect(jsonPath("$.counts.playing").value(1))
                .andExpect(jsonPath("$.counts.followers").value(0));
    }

    @Test
    void unknownUsersAre404() throws Exception {
        mockMvc.perform(get("/api/v1/users/ninguem_aqui"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Ninguém usa o username \"ninguem_aqui\"."));
    }

    private void add(Account account, String slug, String body) throws Exception {
        long gameId = jdbc.sql("SELECT id FROM games WHERE slug = :slug")
                .param("slug", slug)
                .query(Long.class)
                .single();
        send(put("/api/v1/me/library/{gameId}", gameId), account, body).andExpect(status().isCreated());
    }

    private long store(String slug) {
        return jdbc.sql("SELECT id FROM stores WHERE slug = :slug")
                .param("slug", slug)
                .query(Long.class)
                .single();
    }

    private ResultActions send(MockHttpServletRequestBuilder request, Account account, String body) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
    }
}
