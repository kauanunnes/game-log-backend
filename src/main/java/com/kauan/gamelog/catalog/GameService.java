package com.kauan.gamelog.catalog;

import com.kauan.gamelog.catalog.dto.GameDetailsDTO;
import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.shared.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {
    private final GameRepository gameRepository;
    private final GameSearchRepository gameSearchRepository;

    public GameService(GameRepository gameRepository, GameSearchRepository gameSearchRepository) {
        this.gameRepository = gameRepository;
        this.gameSearchRepository = gameSearchRepository;
    }

    @Transactional(readOnly = true)
    public Page<GameSummaryDTO> search(GameSearch search, Pageable pageable) {
        return gameSearchRepository.search(search.normalized(), pageable);
    }

    @Transactional(readOnly = true)
    public GameDetailsDTO findBySlug(String slug) {
        return gameRepository
                .findBySlug(slug)
                .map(GameDetailsDTO::from)
                .orElseThrow(() -> new NotFoundException("Jogo \"" + slug + "\" não encontrado."));
    }
}
