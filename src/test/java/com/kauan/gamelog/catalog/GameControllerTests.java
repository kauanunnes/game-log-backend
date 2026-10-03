package com.kauan.gamelog.catalog;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kauan.gamelog.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class GameControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GenreRepository genreRepository;

    @ParameterizedTest
    @CsvSource({
        "witcher, The Witcher 3: Wild Hunt",
        "wicher, The Witcher 3: Wild Hunt",
        "POKEMON, Pokémon Red",
        "zelda breath, The Legend of Zelda: Breath of the Wild"
    })
    void findsGamesByNameToleratingTyposAndAccents(String query, String title) throws Exception {
        mockMvc.perform(get("/api/v1/games").param("q", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value(title));
    }

    @Test
    void filtersByGenreAndYear() throws Exception {
        Long platformGenre = genreRepository.findAll().stream()
                .filter(genre -> genre.getSlug().equals("platform"))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/v1/games")
                        .param("genreId", platformGenre.toString())
                        .param("year", "2017"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", contains("Hollow Knight")));
    }

    @Test
    void sortsAndPaginates() throws Exception {
        mockMvc.perform(get("/api/v1/games").param("sort", "release").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.content[*].title", contains("Hollow Knight: Silksong", "Baldur's Gate 3", "Elden Ring")))
                .andExpect(jsonPath("$.content[0].releaseYear").value(2025))
                .andExpect(jsonPath("$.page.totalElements").value(16))
                .andExpect(jsonPath("$.page.totalPages").value(6));
    }

    @Test
    void capsThePageSizeAtFifty() throws Exception {
        mockMvc.perform(get("/api/v1/games").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(50));
    }

    @Test
    void rejectsInvalidFilters() throws Exception {
        mockMvc.perform(get("/api/v1/games").param("year", "1800")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/games").param("sort", "random")).andExpect(status().isBadRequest());
    }

    @Test
    void returnsGameDetailsBySlug() throws Exception {
        mockMvc.perform(get("/api/v1/games/hollow-knight"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hollow Knight"))
                .andExpect(jsonPath("$.releaseDate").value("2017-02-24"))
                .andExpect(jsonPath("$.kind").value("MAIN"))
                .andExpect(jsonPath("$.genres[*].name", contains("Adventure", "Indie", "Platform")))
                .andExpect(jsonPath("$.platforms.length()").value(4));
    }

    @Test
    void returnsProblemDetailsForUnknownSlug() throws Exception {
        mockMvc.perform(get("/api/v1/games/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Jogo \"nao-existe\" não encontrado."));
    }

    @Test
    void servesEndpointsOnlyUnderTheApiPrefix() throws Exception {
        mockMvc.perform(get("/games")).andExpect(status().isNotFound());
    }
}
