package com.kauan.games_list.services;

import com.kauan.games_list.dto.GameDTO;
import com.kauan.games_list.dto.GameListDTO;
import com.kauan.games_list.dto.GameMinDTO;
import com.kauan.games_list.entities.Game;
import com.kauan.games_list.entities.GameList;
import com.kauan.games_list.repositories.GameListRepository;
import com.kauan.games_list.repositories.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GameListService {
    @Autowired
    private GameListRepository gameListRepository;
    @Transactional(readOnly = true)
    public List<GameListDTO> findAll() {
        List<GameList> result = gameListRepository.findAll();
        return result.stream().map(GameListDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public GameListDTO findById(Long id) {
        GameList result =  gameListRepository.findById(id).get();
        return new GameListDTO(result);
    }
}
