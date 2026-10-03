package com.kauan.gamelog.lists;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kauan.gamelog.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class GameListControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsAllLists() throws Exception {
        mockMvc.perform(get("/api/v1/lists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aventura e RPG"))
                .andExpect(jsonPath("$[1].name").value("Jogos de plataforma"));
    }

    @Test
    void listsGamesOfAListInPositionOrder() throws Exception {
        mockMvc.perform(get("/api/v1/lists/2/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Super Mario World"))
                .andExpect(jsonPath("$[4].title").value("Sonic CD"))
                .andExpect(jsonPath("$[0].year").isNumber());
    }

    @Test
    void returnsNotFoundForUnknownList() throws Exception {
        mockMvc.perform(get("/api/v1/lists/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Lista 999 não encontrada."));
    }
}
