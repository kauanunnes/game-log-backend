package com.kauan.gamelog.services;

import com.kauan.gamelog.dto.GameDTO;
import com.kauan.gamelog.dto.GameListDTO;
import com.kauan.gamelog.dto.GameMinDTO;
import com.kauan.gamelog.entities.Game;
import com.kauan.gamelog.entities.GameList;
import com.kauan.gamelog.repositories.GameListRepository;
import com.kauan.gamelog.repositories.GameRepository;
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
