package com.kauan.gamelog.export;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
class ExportTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void exportsEverythingTheAccountKeepsIncludingWhatIsPrivate() throws Exception {
        Account ana = Account.register(mockMvc, "ex_ana");
        Account bia = Account.register(mockMvc, "ex_bia");
        send(ana, put("/api/v1/me/library/{gameId}", id("games", "hollow-knight")), """
                {"status": "PLAYED", "favorite": true,
                 "acquisition": {"method": "PURCHASED", "storeId": %d, "price": {"amount": "46.99", "currency": "BRL"}}}
                """.formatted(
                        id("stores", "steam")));
        send(ana, put("/api/v1/me/library/featured"), """
                {"gameIds": [%d]}""".formatted(id("games", "hollow-knight")));
        String list = send(ana, post("/api/v1/me/lists"), """
                {"title": "Metroidvanias", "visibility": "PRIVATE"}""");
        send(ana, put("/api/v1/me/lists/{listId}/items", (Integer) JsonPath.read(list, "$.id")), """
                {"items": [{"gameId": %d, "note": "O mapa."}]}""".formatted(
                        id("games", "hollow-knight")));
        send(ana, put("/api/v1/users/ex_bia/follow"), "");
        send(bia, put("/api/v1/users/ex_ana/follow"), "");
        send(bia, put("/api/v1/me/library/{gameId}", id("games", "celeste")), """
                {"status": "PLAYED", "review": {"rating": 5, "text": "Difícil e gentil."}}""");
        send(ana, put("/api/v1/reviews/{entryId}/like", entry("ex_bia", "celeste")), "");

        as(ana, get("/api/v1/me/export"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                matchesPattern("attachment; filename=\"game-log-ex_ana-\\d{4}-\\d{2}-\\d{2}\\.json\"")))
                .andExpect(jsonPath("$.exportedAt").isNotEmpty())
                .andExpect(jsonPath("$.account.email").value("ex_ana@example.com"))
                .andExpect(jsonPath("$.library[0].game.slug").value("hollow-knight"))
                .andExpect(jsonPath("$.library[0].acquisition.price.amount").value("46.99"))
                .andExpect(jsonPath("$.featured[0].slug").value("hollow-knight"))
                .andExpect(jsonPath("$.lists[0].title").value("Metroidvanias"))
                .andExpect(jsonPath("$.lists[0].items[0].note").value("O mapa."))
                .andExpect(jsonPath("$.following[0].username").value("ex_bia"))
                .andExpect(jsonPath("$.followers[0].username").value("ex_bia"))
                .andExpect(jsonPath("$.likedReviews[0].author").value("ex_bia"))
                .andExpect(jsonPath("$.likedReviews[0].game.slug").value("celeste"));
    }

    @Test
    void anEmptyAccountExportsEmptyLists() throws Exception {
        Account caio = Account.register(mockMvc, "ex_caio");

        as(caio, get("/api/v1/me/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.username").value("ex_caio"))
                .andExpect(jsonPath("$.library").isEmpty())
                .andExpect(jsonPath("$.lists").isEmpty())
                .andExpect(jsonPath("$.likedReviews").isEmpty());
    }

    @Test
    void exportNeedsLogin() throws Exception {
        mockMvc.perform(get("/api/v1/me/export")).andExpect(status().isUnauthorized());
    }

    private String send(Account account, MockHttpServletRequestBuilder request, String body) throws Exception {
        if (!body.isEmpty()) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return as(account, request)
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private long id(String table, String slug) {
        // table vem sempre dos próprios testes
        return jdbc.sql("SELECT id FROM " + table + " WHERE slug = :slug")
                .param("slug", slug)
                .query(Long.class)
                .single();
    }

    private long entry(String username, String slug) {
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
