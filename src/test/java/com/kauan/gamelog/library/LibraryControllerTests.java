package com.kauan.gamelog.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kauan.gamelog.Account;
import com.kauan.gamelog.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
@RecordApplicationEvents
class LibraryControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private ApplicationEvents events;

    @Test
    void requiresLogin() throws Exception {
        mockMvc.perform(get("/api/v1/me/library")).andExpect(status().isUnauthorized());
    }

    @Test
    void putCreatesThenReplacesWithoutDuplicating() throws Exception {
        Account account = Account.register(mockMvc, "lib_ana");
        long celeste = game("celeste");

        send(put("/api/v1/me/library/{gameId}", celeste), account, """
                        {"status": "BACKLOG"}
                        """)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/me/library/" + celeste))
                .andExpect(jsonPath("$.game.title").value("Celeste"))
                .andExpect(jsonPath("$.game.releaseYear").value(2018))
                .andExpect(jsonPath("$.status").value("BACKLOG"))
                .andExpect(jsonPath("$.favorite").value(false));
        send(put("/api/v1/me/library/{gameId}", celeste), account, """
                        {"status": "PLAYING", "favorite": true}
                        """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLAYING"));

        // O banco é dividido entre as classes de teste: conta só as entradas desta conta.
        assertThat(jdbc.sql("""
                                SELECT count(*) FROM library_entries e JOIN users u ON u.id = e.user_id
                                WHERE u.username = 'lib_ana' AND e.game_id = :gameId""").param("gameId", celeste).query(Long.class).single())
                .isEqualTo(1);
        assertThat(events.stream(LibraryEntryChanged.class)).hasSize(2);
    }

    @Test
    void keepsTheReviewThePlaythroughAndThePriceAsSent() throws Exception {
        Account account = Account.register(mockMvc, "lib_bia");

        send(put("/api/v1/me/library/{gameId}", game("hollow-knight")), account, """
                        {
                          "status": "PLAYED",
                          "favorite": true,
                          "review": {"rating": 4.75, "recommends": true, "text": " Exploração incrível. ", "hasSpoilers": false},
                          "playthrough": {"platformId": %d, "hoursPlayed": 42, "startedOn": "2026-01-01",
                                          "finishedOn": "2026-02-01", "completed": true},
                          "acquisition": {"method": "PURCHASED", "storeId": %d,
                                          "price": {"amount": "46.99", "currency": "BRL"}, "acquiredOn": "2025-12-20"}
                        }
                        """.formatted(
                                platform("switch"), store("steam")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.review.rating").value(4.75))
                .andExpect(jsonPath("$.review.text").value("Exploração incrível."))
                .andExpect(jsonPath("$.playthrough.hoursPlayed").value(42))
                .andExpect(jsonPath("$.playthrough.completed").value(true))
                .andExpect(jsonPath("$.acquisition.price.amount").value("46.99"))
                .andExpect(jsonPath("$.acquisition.price.currency").value("BRL"));
    }

    @Test
    void refusesFieldsThatDoNotFitTheStatus() throws Exception {
        Account account = Account.register(mockMvc, "lib_caio");

        send(put("/api/v1/me/library/{gameId}", game("hades")), account, """
                        {"status": "WISHLIST", "review": {"rating": 5}}
                        """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_FIELDS_FOR_STATUS"))
                .andExpect(jsonPath("$.detail").value("A entrada não é válida para o status Lista de desejos."))
                .andExpect(jsonPath("$.errors[0].field").value("review"))
                .andExpect(
                        jsonPath("$.errors[0].message").value("Avaliação só vale em Jogando, Jogado ou Abandonado."));
    }

    @Test
    void validatesTheFormatOfEachField() throws Exception {
        Account account = Account.register(mockMvc, "lib_davi");
        long hades = game("hades");

        send(put("/api/v1/me/library/{gameId}", hades), account, """
                        {"status": "PLAYED", "review": {"rating": 4.8}}
                        """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("review.rating"));
        send(put("/api/v1/me/library/{gameId}", hades), account, """
                        {"status": "PLAYING", "playthrough": {"startedOn": "2999-01-01"}}
                        """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("playthrough.startedOn"))
                .andExpect(jsonPath("$.errors[0].message").value("não pode estar no futuro"));
        send(put("/api/v1/me/library/{gameId}", hades), account, """
                        {"status": "PLAYING", "playthrough": {"platformId": 999999}}
                        """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("UNKNOWN_REFERENCE"));
        send(put("/api/v1/me/library/{gameId}", 999999), account, """
                        {"status": "PLAYING"}
                        """).andExpect(status().isNotFound());
    }

    @Test
    void patchChangesOnlyWhatWasSentAndChecksTheFinalState() throws Exception {
        Account account = Account.register(mockMvc, "lib_eva");
        long sekiro = game("sekiro-shadows-die-twice");
        send(put("/api/v1/me/library/{gameId}", sekiro), account, """
                        {"status": "PLAYING", "review": {"rating": 4}, "playthrough": {"hoursPlayed": 10}}
                        """).andExpect(status().isCreated());

        send(patch("/api/v1/me/library/{gameId}", sekiro), account, """
                        {"status": "PLAYED", "playthrough": {"finishedOn": "2026-03-01", "completed": true}}
                        """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLAYED"))
                .andExpect(jsonPath("$.review.rating").value(4))
                .andExpect(jsonPath("$.playthrough.hoursPlayed").value(10))
                .andExpect(jsonPath("$.playthrough.completed").value(true));

        // Vale o estado final: com avaliação, o jogo não volta para a lista de desejos.
        send(patch("/api/v1/me/library/{gameId}", sekiro), account, """
                        {"status": "WISHLIST"}
                        """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_FIELDS_FOR_STATUS"));
        send(patch("/api/v1/me/library/{gameId}", sekiro), account, """
                        {"status": "WISHLIST", "review": null, "playthrough": null}
                        """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review").doesNotExist());
    }

    @Test
    void missingEntriesAre404() throws Exception {
        Account account = Account.register(mockMvc, "lib_fabi");
        long portal = game("portal-2");

        send(get("/api/v1/me/library/{gameId}", portal), account, null).andExpect(status().isNotFound());
        send(patch("/api/v1/me/library/{gameId}", portal), account, """
                        {"status": "PLAYED"}
                        """).andExpect(status().isNotFound());
        send(delete("/api/v1/me/library/{gameId}", portal), account, null).andExpect(status().isNotFound());
    }

    @Test
    void deletesTheEntry() throws Exception {
        Account account = Account.register(mockMvc, "lib_gil");
        long portal = game("portal-2");
        send(put("/api/v1/me/library/{gameId}", portal), account, """
                        {"status": "WISHLIST"}
                        """).andExpect(status().isCreated());

        send(delete("/api/v1/me/library/{gameId}", portal), account, null).andExpect(status().isNoContent());
        send(get("/api/v1/me/library/{gameId}", portal), account, null).andExpect(status().isNotFound());
    }

    @Test
    void listsWithFiltersAndSorting() throws Exception {
        Account account = Account.register(mockMvc, "lib_hugo");
        add(account, "hollow-knight", """
                {"status": "PLAYED", "favorite": true, "review": {"rating": 5, "recommends": true},
                 "playthrough": {"completed": true}}""");
        add(account, "celeste", """
                {"status": "PLAYED", "review": {"rating": 4.5, "recommends": true}}""");
        add(account, "hades", """
                {"status": "DROPPED", "review": {"rating": 2, "recommends": false}}""");
        add(account, "elden-ring", """
                {"status": "WISHLIST"}""");

        list(account, "?status=PLAYED,DROPPED&sort=rating,desc")
                .andExpect(jsonPath("$.content[*].game.title", contains("Hollow Knight", "Celeste", "Hades")))
                .andExpect(jsonPath("$.page.totalElements").value(3));
        list(account, "?favorite=true").andExpect(jsonPath("$.content[*].game.title", contains("Hollow Knight")));
        list(account, "?recommends=true&minRating=4.75")
                .andExpect(jsonPath("$.content[*].game.title", contains("Hollow Knight")));
        list(account, "?q=celes").andExpect(jsonPath("$.content[*].game.title", contains("Celeste")));
        list(account, "?status=PLAYED&completed=true")
                .andExpect(jsonPath("$.content[*].game.title", contains("Hollow Knight")));
        list(account, "?status=PLAYED&completed=false")
                .andExpect(jsonPath("$.content[*].game.title", contains("Celeste")));
        list(account, "?genreId=" + genre("platform") + "&sort=title")
                .andExpect(jsonPath("$.content[*].game.title", contains("Celeste", "Hollow Knight")));
        list(account, "?sort=preco").andExpect(status().isUnprocessableContent());
    }

    @Test
    void deletingTheAccountDeletesTheLibrary() throws Exception {
        Account account = Account.register(mockMvc, "lib_iris");
        add(account, "stardew-valley", """
                {"status": "PLAYING"}""");
        long userId = jdbc.sql("SELECT id FROM users WHERE username = 'lib_iris'")
                .query(Long.class)
                .single();

        send(delete("/api/v1/me"), account, """
                        {"password": "%s"}
                        """.formatted(Account.PASSWORD)).andExpect(status().isNoContent());

        assertThat(jdbc.sql("SELECT count(*) FROM library_entries WHERE user_id = :userId")
                        .param("userId", userId)
                        .query(Long.class)
                        .single())
                .isZero();
    }

    private void add(Account account, String slug, String body) throws Exception {
        send(put("/api/v1/me/library/{gameId}", game(slug)), account, body).andExpect(status().isCreated());
    }

    private ResultActions list(Account account, String query) throws Exception {
        return send(get("/api/v1/me/library" + query), account, null);
    }

    private ResultActions send(MockHttpServletRequestBuilder request, Account account, String body) throws Exception {
        request.header(HttpHeaders.AUTHORIZATION, account.bearer());
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }

    private long game(String slug) {
        return id("games", slug);
    }

    private long platform(String slug) {
        return id("platforms", slug);
    }

    private long store(String slug) {
        return id("stores", slug);
    }

    private long genre(String slug) {
        return id("genres", slug);
    }

    private long id(String table, String slug) {
        return jdbc.sql("SELECT id FROM " + table + " WHERE slug = :slug")
                .param("slug", slug)
                .query(Long.class)
                .single();
    }
}
