package com.kauan.gamelog.catalog;

import com.kauan.gamelog.catalog.dto.GameDetailsDTO;
import com.kauan.gamelog.catalog.dto.GameProfile;
import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.catalog.igdb.IgdbCatalogSync;
import com.kauan.gamelog.shared.NotFoundException;
import com.kauan.gamelog.shared.UnprocessableException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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
    private final GameCommunity community;
    private final GameProfiles profiles;

    public GameService(
            GameRepository gameRepository,
            GameSearchRepository gameSearchRepository,
            IgdbCatalogSync igdbCatalogSync,
            GameCommunity community,
            GameProfiles profiles) {
        this.gameRepository = gameRepository;
        this.gameSearchRepository = gameSearchRepository;
        this.igdbCatalogSync = igdbCatalogSync;
        this.community = community;
        this.profiles = profiles;
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
                .map(game -> GameDetailsDTO.from(game, community.of(game.getId())))
                .orElseThrow(() -> new NotFoundException("Jogo \"" + slug + "\" não encontrado."));
    }

    /** RF26: importa ou atualiza pelo id do IGDB. */
    public GameDetailsDTO importFromIgdb(long igdbId) {
        long id = igdbCatalogSync
                .importById(igdbId)
                .orElseThrow(() ->
                        new NotFoundException("O IGDB não tem um jogo com id " + igdbId + " que entre no catálogo."));
        return findById(id);
    }

    /** RF26: busca de novo no IGDB os dados de um jogo que já está no catálogo. */
    public GameDetailsDTO syncWithIgdb(long id) {
        Long igdbId = gameRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Jogo " + id + " não encontrado."))
                .getIgdbId();
        if (igdbId == null) {
            throw UnprocessableException.field("NOT_FROM_IGDB", "id", "esse jogo não veio do IGDB");
        }
        return importFromIgdb(igdbId);
    }

    /** Id do jogo dono do slug, para rotas como {@code /games/{slug}/reviews}. */
    @Transactional(readOnly = true)
    public long idOf(String slug) {
        return gameRepository
                .findIdBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Jogo \"" + slug + "\" não encontrado."));
    }

    @Transactional(readOnly = true)
    public boolean exists(long id) {
        return gameRepository.existsById(id);
    }

    /** O que descreve cada um destes jogos, numa consulta só. */
    @Transactional(readOnly = true)
    public List<GameProfile> profiles(Collection<Long> ids) {
        return profiles.of(ids);
    }

    /** Para percorrer o catálogo inteiro: os próximos {@code limit} ids depois de {@code afterId}, em ordem. */
    @Transactional(readOnly = true)
    public List<Long> idsAfter(long afterId, int limit) {
        return profiles.idsAfter(afterId, limit);
    }

    /** Quais destes ids existem no catálogo, numa consulta só. */
    @Transactional(readOnly = true)
    public Set<Long> existing(Collection<Long> ids) {
        return ids.isEmpty() ? Set.of() : gameRepository.findExistingIds(ids);
    }

    @Transactional(readOnly = true)
    public GameDetailsDTO findById(long id) {
        return gameRepository
                .findDetailedById(id)
                .map(game -> GameDetailsDTO.from(game, community.of(game.getId())))
                .orElseThrow(() -> new NotFoundException("Jogo " + id + " não encontrado."));
    }
}
