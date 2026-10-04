package com.kauan.gamelog;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class OperationsTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void reportsHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void onlyAdminsSeeTheMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/genres")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        Account user = Account.register(mockMvc, "op_user");
        mockMvc.perform(get("/actuator/metrics").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isForbidden());

        Account admin = Account.admin(mockMvc, jdbc, "op_admin");
        mockMvc.perform(get("/actuator/metrics").header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names", hasItems("http.server.requests", "cache.gets")));
        mockMvc.perform(get("/actuator/metrics/cache.gets")
                        .param("tag", "cache:lookups")
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void publishesOpenApiDocsAtTheRoot() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/games']").exists());
    }
}
