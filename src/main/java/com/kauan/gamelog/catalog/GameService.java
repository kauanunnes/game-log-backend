package com.kauan.gamelog.catalog;

import com.kauan.gamelog.catalog.dto.GameDTO;
import com.kauan.gamelog.catalog.dto.GameMinDTO;
import com.kauan.gamelog.shared.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Game result =
                gameRepository.findById(id).orElseThrow(() -> new NotFoundException("Jogo " + id + " não encontrado."));
        return new GameDTO(result);
    }
}
