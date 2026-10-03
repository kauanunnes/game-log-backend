package com.kauan.gamelog.services;

import com.kauan.gamelog.dto.GameDTO;
import com.kauan.gamelog.dto.GameMinDTO;
import com.kauan.gamelog.entities.Game;
import com.kauan.gamelog.errors.NotFoundException;
import com.kauan.gamelog.projections.GameMinProjection;
import com.kauan.gamelog.repositories.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GameService {
    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Transactional(readOnly = true)
    public List<GameMinDTO> findAll() {
        List<Game> result = gameRepository.findAll();
        return result.stream().map(GameMinDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public GameDTO findById(Long id) {
        Game result = gameRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Jogo " + id + " não encontrado."));
        return new GameDTO(result);
    }

    @Transactional(readOnly = true)
    public List<GameMinDTO> findByList(Long listId) {
        List<GameMinProjection> result = gameRepository.searchByList(listId);
        return result.stream().map(GameMinDTO::new).toList();
    }
}
