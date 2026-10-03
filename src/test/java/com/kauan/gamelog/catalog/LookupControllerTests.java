package com.kauan.gamelog.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kauan.gamelog.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class LookupControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsGenresByName() throws Exception {
        mockMvc.perform(get("/api/v1/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[0].name").value("Adventure"));
    }

    @Test
    void listsPlatformsByName() throws Exception {
        mockMvc.perform(get("/api/v1/platforms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(11))
                .andExpect(jsonPath("$[0].name").value("Game Boy"))
                .andExpect(jsonPath("$[0].abbreviation").value("GB"));
    }

    @Test
    void listsStoresWithOtherLast() throws Exception {
        mockMvc.perform(get("/api/v1/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(11))
                .andExpect(jsonPath("$[0].name").value("Steam"))
                .andExpect(jsonPath("$[10].name").value("Outra"));
    }
}
