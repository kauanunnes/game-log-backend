package com.kauan.gamelog.controllers;

import com.kauan.gamelog.dto.GameDTO;
import com.kauan.gamelog.dto.GameMinDTO;
import com.kauan.gamelog.entities.Game;
import com.kauan.gamelog.services.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/games")
public class GameController {
    @Autowired
    private GameService gameService;
    @GetMapping(value = "/{id}")
    public GameDTO findById(@PathVariable Long id) {
        return gameService.findById(id);
    }
    @GetMapping
    public List<GameMinDTO> findAll() {
        return gameService.findAll();
    }

}
