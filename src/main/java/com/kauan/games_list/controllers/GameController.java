package com.kauan.games_list.controllers;

import com.kauan.games_list.dto.GameMinDTO;
import com.kauan.games_list.entities.Game;
import com.kauan.games_list.services.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/games")
public class GameController {
    @Autowired
    private GameService gameService;

    @GetMapping(value = "/")
    public List<GameMinDTO> findAll() {
        return gameService.findAll();
    }
}
