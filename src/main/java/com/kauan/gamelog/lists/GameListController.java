package com.kauan.gamelog.lists;

import com.kauan.gamelog.catalog.dto.GameMinDTO;
import com.kauan.gamelog.lists.dto.GameListDTO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/lists")
public class GameListController {
    private final GameListService gameListService;

    public GameListController(GameListService gameListService) {
        this.gameListService = gameListService;
    }

    @GetMapping(value = "/{id}")
    public GameListDTO findById(@PathVariable Long id) {
        return gameListService.findById(id);
    }

    @GetMapping(value = "/{listId}/games")
    public List<GameMinDTO> findGames(@PathVariable Long listId) {
        return gameListService.findGames(listId);
    }

    @GetMapping
    public List<GameListDTO> findAll() {
        return gameListService.findAll();
    }
}
