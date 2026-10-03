package com.kauan.gamelog.controllers;

import com.kauan.gamelog.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class GameControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsAllGames() throws Exception {
        mockMvc.perform(get("/api/v1/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].title").value("Mass Effect Trilogy"));
    }

    @Test
    void findsGameById() throws Exception {
        mockMvc.perform(get("/api/v1/games/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hollow Knight"))
                .andExpect(jsonPath("$.longDescription").isNotEmpty());
    }

    @Test
    void returnsProblemDetailsForUnknownGame() throws Exception {
        mockMvc.perform(get("/api/v1/games/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Não encontrado"))
                .andExpect(jsonPath("$.detail").value("Jogo 999 não encontrado."));
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
    void servesEndpointsOnlyUnderTheApiPrefix() throws Exception {
        mockMvc.perform(get("/games")).andExpect(status().isNotFound());
    }
}
