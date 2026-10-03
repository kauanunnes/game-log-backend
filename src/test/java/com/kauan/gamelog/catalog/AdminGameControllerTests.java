package com.kauan.gamelog.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** Aqui o IGDB está desligado; a importação de verdade é testada em {@code IgdbImportTests}. */
@IntegrationTest
class AdminGameControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void requiresAnAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/admin/games/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"igdbId\": 1942}"))
                .andExpect(status().isUnauthorized());

        importGame(Account.register(mockMvc, "adm_ana"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Sem permissão"));
    }

    @Test
    void answers503WhileIgdbIsNotConfigured() throws Exception {
        importGame(Account.admin(mockMvc, jdbc, "adm_bia"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("IGDB indisponível"));
    }

    @Test
    void doesNotSyncAGameThatDidNotComeFromIgdb() throws Exception {
        Account admin = Account.admin(mockMvc, jdbc, "adm_caio");
        long celeste = jdbc.sql("SELECT id FROM games WHERE slug = 'celeste'")
                .query(Long.class)
                .single();

        mockMvc.perform(post("/api/v1/admin/games/{id}/sync", celeste)
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("NOT_FROM_IGDB"));
    }

    private ResultActions importGame(Account account) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/games/import")
                .header(HttpHeaders.AUTHORIZATION, account.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"igdbId\": 1942}"));
    }
}
