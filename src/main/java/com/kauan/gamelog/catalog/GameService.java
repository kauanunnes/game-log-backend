package com.kauan.gamelog.catalog;

import com.kauan.gamelog.catalog.dto.GameDetailsDTO;
import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.catalog.igdb.IgdbCatalogSync;
import com.kauan.gamelog.shared.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {
    /** Abaixo disso, a primeira página de uma busca por texto também consulta o IGDB. */
    private static final int WEAK_RESULT_COUNT = 5;

    private final GameRepository gameRepository;
    private final GameSearchRepository gameSearchRepository;
    private final IgdbCatalogSync igdbCatalogSync;

    public GameService(
            GameRepository gameRepository, GameSearchRepository gameSearchRepository, IgdbCatalogSync igdbCatalogSync) {
        this.gameRepository = gameRepository;
        this.gameSearchRepository = gameSearchRepository;
        this.igdbCatalogSync = igdbCatalogSync;
    }

    public Page<GameSummaryDTO> search(GameSearch search, Pageable pageable) {
        GameSearch normalized = search.normalized();
        Page<GameSummaryDTO> page = gameSearchRepository.search(normalized, pageable);
        boolean weak =
                normalized.q() != null && pageable.getPageNumber() == 0 && page.getTotalElements() < WEAK_RESULT_COUNT;
        if (weak && igdbCatalogSync.importSearch(normalized.q())) {
            page = gameSearchRepository.search(normalized, pageable);
        }
        return page;
    }

    @Transactional(readOnly = true)
    public GameDetailsDTO findBySlug(String slug) {
        return gameRepository
                .findBySlug(slug)
                .map(GameDetailsDTO::from)
                .orElseThrow(() -> new NotFoundException("Jogo \"" + slug + "\" não encontrado."));
    }
}
