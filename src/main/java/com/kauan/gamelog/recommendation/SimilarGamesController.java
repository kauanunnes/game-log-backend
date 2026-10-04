package com.kauan.gamelog.recommendation;

import com.kauan.gamelog.recommendation.dto.SimilarGamesDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimilarGamesController {
    private final SimilarGames similarGames;

    public SimilarGamesController(SimilarGames similarGames) {
        this.similarGames = similarGames;
    }

    @GetMapping("/games/{slug}/similar")
    public SimilarGamesDTO similar(@PathVariable String slug) {
        return similarGames.of(slug);
    }
}
