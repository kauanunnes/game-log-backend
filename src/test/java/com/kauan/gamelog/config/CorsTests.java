package com.kauan.gamelog.config;

import com.kauan.gamelog.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class CorsTests {
    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "http://localhost:3000"})
    void allowsConfiguredOrigins(String origin) throws Exception {
        preflight(origin)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
    }

    @Test
    void rejectsOtherOrigins() throws Exception {
        preflight("https://evil.example").andExpect(status().isForbidden());
    }

    private ResultActions preflight(String origin) throws Exception {
        return mockMvc.perform(options("/api/v1/games")
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"));
    }
}
