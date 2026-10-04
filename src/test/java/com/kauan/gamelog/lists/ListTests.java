package com.kauan.gamelog.lists;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kauan.gamelog.Account;
import com.kauan.gamelog.IntegrationTest;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class ListTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void createsFillsAndReordersAList() throws Exception {
        Account ana = Account.register(mockMvc, "ls_ana");
        long list = create(ana, """
                {"title": "  Top metroidvanias  ", "description": "Os melhores."}""");

        items(ana, list, """
                        {"items": [{"gameId": %d}, {"gameId": %d, "note": "O mapa é uma obra."}, {"gameId": %d}]}
                        """.formatted(game("celeste"), game("hollow-knight"), game("hades")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Top metroidvanias"))
                .andExpect(jsonPath("$.items[*].game.slug", contains("celeste", "hollow-knight", "hades")))
                .andExpect(jsonPath("$.items[*].position", contains(1, 2, 3)))
                .andExpect(jsonPath("$.items[1].note").value("O mapa é uma obra."));

        items(ana, list, """
                        {"items": [{"gameId": %d}, {"gameId": %d}]}""".formatted(game("hades"), game("celeste")))
                .andExpect(jsonPath("$.items[*].game.slug", contains("hades", "celeste")));

        as(ana, get("/api/v1/me/lists"))
                .andExpect(jsonPath("$.content[0].id").value(list))
                .andExpect(jsonPath("$.content[0].itemCount").value(2))
                .andExpect(jsonPath("$.content[0].preview[*].slug", contains("hades", "celeste")))
                .andExpect(jsonPath("$.content[0].visibility").value("PUBLIC"));
    }

    @Test
    void patchChangesOnlyWhatComes() throws Exception {
        Account bia = Account.register(mockMvc, "ls_bia");
        long list = create(bia, """
                {"title": "Para jogar em dupla", "description": "Sofá e controle."}""");

        change(bia, list, """
                        {"title": "Cooperativos"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Cooperativos"))
                .andExpect(jsonPath("$.description").value("Sofá e controle."));
        change(bia, list, """
                        {"description": null, "visibility": "PRIVATE"}""")
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));
        change(bia, list, """
                        {"title": " "}""").andExpect(status().isUnprocessableContent());
    }

    @Test
    void refusesRepeatedUnknownAndTooManyGames() throws Exception {
        Account caio = Account.register(mockMvc, "ls_caio");
        long list = create(caio, """
                {"title": "Teste"}""");

        items(caio, list, """
                        {"items": [{"gameId": %d}, {"gameId": %d}]}""".formatted(game("celeste"), game("celeste")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("DUPLICATE_GAME"));
        items(caio, list, """
                        {"items": [{"gameId": %d}, {"gameId": 987654321}]}""".formatted(game("celeste")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("UNKNOWN_REFERENCE"))
                .andExpect(jsonPath("$.errors[0].field").value("items[1].gameId"));
        String tooMany = IntStream.rangeClosed(1, 101)
                .mapToObj(i -> "{\"gameId\": " + i + "}")
                .collect(Collectors.joining(",", "{\"items\": [", "]}"));
        items(caio, list, tooMany).andExpect(status().isUnprocessableContent());
    }

    @Test
    void onlyTheOwnerSeesAndEditsThroughMe() throws Exception {
        Account davi = Account.register(mockMvc, "ls_davi");
        Account eva = Account.register(mockMvc, "ls_eva");
        long list = create(davi, """
                {"title": "Minha"}""");

        as(eva, get("/api/v1/me/lists/{listId}", list)).andExpect(status().isNotFound());
        change(eva, list, """
                        {"title": "Roubada"}""").andExpect(status().isNotFound());
        items(eva, list, """
                        {"items": []}""").andExpect(status().isNotFound());
        as(eva, delete("/api/v1/me/lists/{listId}", list)).andExpect(status().isNotFound());

        as(davi, delete("/api/v1/me/lists/{listId}", list)).andExpect(status().isNoContent());
        as(davi, get("/api/v1/me/lists/{listId}", list)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/me/lists")).andExpect(status().isUnauthorized());
    }

    @Test
    void visitorsSeeOnlyPublicListsOfPublicProfiles() throws Exception {
        Account fabi = Account.register(mockMvc, "ls_fabi");
        long open = create(fabi, """
                {"title": "Aberta"}""");
        long hidden = create(fabi, """
                {"title": "Só minha", "visibility": "PRIVATE"}""");

        mockMvc.perform(get("/api/v1/users/ls_fabi/lists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains((int) open)));
        mockMvc.perform(get("/api/v1/users/ls_fabi/lists/{listId}", open))
                .andExpect(jsonPath("$.owner.username").value("ls_fabi"));
        mockMvc.perform(get("/api/v1/users/ls_fabi/lists/{listId}", hidden)).andExpect(status().isNotFound());

        as(
                        fabi,
                        patch("/api/v1/me/settings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {"profileVisibility": "PRIVATE"}"""))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/ls_fabi/lists")).andExpect(status().isForbidden());
    }

    private long create(Account account, String body) throws Exception {
        String json = as(
                        account,
                        post("/api/v1/me/lists")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private ResultActions items(Account account, long listId, String body) throws Exception {
        return as(
                account,
                put("/api/v1/me/lists/{listId}/items", listId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    private ResultActions change(Account account, long listId, String body) throws Exception {
        return as(
                account,
                patch("/api/v1/me/lists/{listId}", listId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
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
