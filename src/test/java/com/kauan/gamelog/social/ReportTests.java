package com.kauan.gamelog.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class ReportTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void groupsTheOpenReportsOfAReviewForTheAdmins() throws Exception {
        Account ana = Account.register(mockMvc, "rp_ana");
        Account bia = Account.register(mockMvc, "rp_bia");
        Account caio = Account.register(mockMvc, "rp_caio");
        Account admin = Account.admin(mockMvc, jdbc, "rp_admin");
        long entry = review(ana, "hollow-knight", "Compre agora no meu site!");

        report(bia, entry, "SPAM", null).andExpect(status().isCreated());
        report(caio, entry, "OTHER", "Propaganda disfarçada.").andExpect(status().isCreated());

        String mine = "$.content[?(@.review.id == " + entry + ")]";
        as(admin, get("/api/v1/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(mine + ".review.text", contains("Compre agora no meu site!")))
                .andExpect(jsonPath(mine + ".reports[*].reporter", contains("rp_bia", "rp_caio")))
                .andExpect(jsonPath(mine + ".reports[1].details", contains("Propaganda disfarçada.")));
        as(bia, get("/api/v1/admin/reports")).andExpect(status().isForbidden());
    }

    @Test
    void oneOpenReportPerPersonAndNeverTheOwnReview() throws Exception {
        Account davi = Account.register(mockMvc, "rp_davi");
        Account eva = Account.register(mockMvc, "rp_eva");
        long entry = review(davi, "portal-2", "Perfeito.");
        save(eva, "portal-2", """
                {"status": "PLAYED", "review": {"rating": 3}}""");

        report(eva, entry, "OFFENSIVE", null).andExpect(status().isCreated());
        report(eva, entry, "SPAM", null).andExpect(status().isConflict());
        report(davi, entry, "SPAM", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CANNOT_REPORT_OWN_REVIEW"));
        report(davi, entryId("rp_eva", "portal-2"), "SPAM", null).andExpect(status().isNotFound());
        report(davi, entry, "NONSENSE", null).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/reviews/{entryId}/reports", entry)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "SPAM"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void keepingClosesTheReportsAndTheReviewStays() throws Exception {
        Account fabi = Account.register(mockMvc, "rp_fabi");
        Account gil = Account.register(mockMvc, "rp_gil");
        Account admin = Account.admin(mockMvc, jdbc, "rp_admin2");
        long entry = review(fabi, "chrono-trigger", "O melhor RPG.");
        report(gil, entry, "SPOILER", null).andExpect(status().isCreated());
        long reportId = reportId("rp_gil", entry);

        resolve(admin, reportId, "KEEP").andExpect(status().isNoContent());
        resolve(admin, reportId, "KEEP").andExpect(status().isConflict());

        as(admin, get("/api/v1/admin/reports"))
                .andExpect(jsonPath("$.content[?(@.review.id == " + entry + ")]", empty()));
        mockMvc.perform(get("/api/v1/users/rp_fabi/reviews"))
                .andExpect(jsonPath("$.content[0].id").value(entry));
        report(gil, entry, "SPOILER", "De novo.").andExpect(status().isCreated());
    }

    @Test
    void removingTakesTheTextAndTheLikesButKeepsTheRating() throws Exception {
        Account hana = Account.register(mockMvc, "rp_hana");
        Account ivo = Account.register(mockMvc, "rp_ivo");
        Account admin = Account.admin(mockMvc, jdbc, "rp_admin3");
        long entry = review(hana, "elden-ring", "Texto ofensivo.");
        as(ivo, put("/api/v1/reviews/{entryId}/like", entry)).andExpect(status().isNoContent());
        report(ivo, entry, "OFFENSIVE", null).andExpect(status().isCreated());

        resolve(admin, reportId("rp_ivo", entry), "REMOVE").andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/rp_hana/reviews")).andExpect(jsonPath("$.content", empty()));
        as(hana, get("/api/v1/me/library/{gameId}", gameId("elden-ring")))
                .andExpect(jsonPath("$.review.rating").value(4))
                .andExpect(jsonPath("$.review.text").doesNotExist());
        long likes = jdbc.sql("SELECT count(*) FROM review_likes WHERE entry_id = :entryId")
                .param("entryId", entry)
                .query(Long.class)
                .single();
        assertThat(likes).isZero();
    }

    @Test
    void theAuthorRemovingTheTextClosesTheReports() throws Exception {
        Account jade = Account.register(mockMvc, "rp_jade");
        Account kai = Account.register(mockMvc, "rp_kai");
        Account admin = Account.admin(mockMvc, jdbc, "rp_admin4");
        long entry = review(jade, "stardew-valley", "Tranquilo.");
        report(kai, entry, "OTHER", null).andExpect(status().isCreated());

        save(jade, "stardew-valley", """
                {"status": "PLAYED", "review": {"rating": 4}}""");

        as(admin, get("/api/v1/admin/reports"))
                .andExpect(jsonPath("$.content[?(@.review.id == " + entry + ")]", empty()));
        resolve(admin, reportId("rp_kai", entry), "KEEP").andExpect(status().isConflict());
    }

    private ResultActions report(Account account, long entryId, String reason, String details) throws Exception {
        String body = details == null
                ? "{\"reason\": \"%s\"}".formatted(reason)
                : "{\"reason\": \"%s\", \"details\": \"%s\"}".formatted(reason, details);
        return as(
                account,
                post("/api/v1/reviews/{entryId}/reports", entryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    private ResultActions resolve(Account admin, long reportId, String decision) throws Exception {
        return as(
                admin,
                patch("/api/v1/admin/reports/{id}", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\": \"%s\"}".formatted(decision)));
    }

    private long review(Account account, String slug, String text) throws Exception {
        save(account, slug, """
                {"status": "PLAYED", "review": {"rating": 4, "text": "%s"}}""".formatted(text));
        return entryId(account.username(), slug);
    }

    private void save(Account account, String slug, String body) throws Exception {
        as(
                        account,
                        put("/api/v1/me/library/{gameId}", gameId(slug))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().is2xxSuccessful());
    }

    private long gameId(String slug) {
        return jdbc.sql("SELECT id FROM games WHERE slug = :slug")
                .param("slug", slug)
                .query(Long.class)
                .single();
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

    private long reportId(String reporter, long entryId) {
        return jdbc.sql("""
                        SELECT r.id FROM reports r JOIN users u ON u.id = r.reporter_id
                        WHERE u.username = :reporter AND r.entry_id = :entryId
                        ORDER BY r.id DESC LIMIT 1
                        """)
                .param("reporter", reporter)
                .param("entryId", entryId)
                .query(Long.class)
                .single();
    }

    private ResultActions as(Account account, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, account.bearer()));
    }
}
