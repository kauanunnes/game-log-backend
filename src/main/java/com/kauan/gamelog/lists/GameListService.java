package com.kauan.gamelog.lists;

import com.kauan.gamelog.catalog.dto.GameMinDTO;
import com.kauan.gamelog.lists.dto.GameListDTO;
import com.kauan.gamelog.shared.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public List<GameMinDTO> findGames(Long listId) {
        return gameListRepository.searchGames(listId).stream()
                .map(game -> new GameMinDTO(
                        game.getId(),
                        game.getGameYear(),
                        game.getTitle(),
                        game.getImgUrl(),
                        game.getShortDescription()))
                .toList();
    }
}
