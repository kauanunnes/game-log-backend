package com.kauan.gamelog.services;

import com.kauan.gamelog.dto.GameListDTO;
import com.kauan.gamelog.entities.GameList;
import com.kauan.gamelog.errors.NotFoundException;
import com.kauan.gamelog.repositories.GameListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GameListService {
    private final GameListRepository gameListRepository;

    public GameListService(GameListRepository gameListRepository) {
        this.gameListRepository = gameListRepository;
    }

    @Transactional(readOnly = true)
    public List<GameListDTO> findAll() {
        List<GameList> result = gameListRepository.findAll();
        return result.stream().map(GameListDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public GameListDTO findById(Long id) {
        GameList result = gameListRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Lista " + id + " não encontrada."));
        return new GameListDTO(result);
    }
}
