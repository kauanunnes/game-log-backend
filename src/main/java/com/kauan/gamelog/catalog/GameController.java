package com.kauan.gamelog.catalog;

import com.kauan.gamelog.catalog.dto.GameDetailsDTO;
import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/games")
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public PagedModel<GameSummaryDTO> search(@Valid GameSearch search, Pageable pageable) {
        return new PagedModel<>(gameService.search(search, pageable));
    }

    @GetMapping("/{slug}")
    public GameDetailsDTO findBySlug(@PathVariable String slug) {
        return gameService.findBySlug(slug);
    }
}
