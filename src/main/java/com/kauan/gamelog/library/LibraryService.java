package com.kauan.gamelog.library;

import com.kauan.gamelog.catalog.GameService;
import com.kauan.gamelog.catalog.LookupService;
import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.library.dto.LibraryCounts;
import com.kauan.gamelog.library.dto.LibraryEntryDTO;
import com.kauan.gamelog.library.dto.LibraryEntryRequest;
import com.kauan.gamelog.library.dto.LibraryFilter;
import com.kauan.gamelog.library.dto.PublicReviewDTO;
import com.kauan.gamelog.library.dto.StatsDTO;
import com.kauan.gamelog.shared.Caches;
import com.kauan.gamelog.shared.ConflictException;
import com.kauan.gamelog.shared.FieldIssue;
import com.kauan.gamelog.shared.JsonMergePatch;
import com.kauan.gamelog.shared.NotFoundException;
import com.kauan.gamelog.shared.UnprocessableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.JsonNode;

@Service
public class LibraryService {
    private final LibraryEntryRepository entries;
    private final LibraryQueries queries;
    private final LibraryStats stats;
    private final CommunityQueries community;
    private final GameService games;
    private final LookupService lookups;
    private final JsonMergePatch mergePatch;
    private final ApplicationEventPublisher events;

    LibraryService(
            LibraryEntryRepository entries,
            LibraryQueries queries,
            LibraryStats stats,
            CommunityQueries community,
            GameService games,
            LookupService lookups,
            JsonMergePatch mergePatch,
            ApplicationEventPublisher events) {
        this.entries = entries;
        this.queries = queries;
        this.stats = stats;
        this.community = community;
        this.games = games;
        this.lookups = lookups;
        this.mergePatch = mergePatch;
        this.events = events;
    }

    /** @param created se o PUT criou a entrada (201) em vez de substituí-la (200) */
    public record Saved(LibraryEntryDTO entry, boolean created) {}

    @Transactional(readOnly = true)
    public Page<LibraryEntryDTO> list(long userId, LibraryFilter filter, Pageable pageable) {
        return queries.find(userId, filter, pageable);
    }

    /** As avaliações de uma pessoa, das editadas por último; quem expõe numa rota pública confere o perfil. */
    @Transactional(readOnly = true)
    public Page<PublicReviewDTO> reviewsBy(long userId, Pageable pageable) {
        return community.reviewsBy(userId, pageable);
    }

    /**
     * Estatísticas completas; quem expõe numa rota pública decide se mostra os gastos. As do ano todo ficam em cache
     * até a biblioteca mudar; as de um ano são calculadas na hora.
     */
    @Cacheable(cacheNames = Caches.STATS, key = "#userId", condition = "#year == null")
    @Transactional(readOnly = true)
    public StatsDTO stats(long userId, Integer year) {
        return stats.of(userId, year);
    }

    @TransactionalEventListener
    @CacheEvict(cacheNames = Caches.STATS, key = "#event.userId()")
    public void forgetStats(LibraryEntryChanged event) {}

    /** RF24: avaliações públicas de um jogo, das mais recentes ou das mais curtidas. */
    @Transactional(readOnly = true)
    public Page<PublicReviewDTO> reviewsOf(String gameSlug, ReviewSort sort, Pageable pageable) {
        return community.reviews(games.idOf(gameSlug), sort, pageable);
    }

    /** Avaliações públicas recentes do site todo (página inicial). */
    @Transactional(readOnly = true)
    public Page<PublicReviewDTO> recentReviews(Pageable pageable) {
        return community.reviews(null, ReviewSort.RECENT, pageable);
    }

    /** Para a moderação: as avaliações destas entradas, de qualquer perfil. */
    @Transactional(readOnly = true)
    public List<PublicReviewDTO> reviewsByIds(Collection<Long> entryIds) {
        return community.reviewsByIds(entryIds);
    }

    /**
     * Moderação (RF53): tira o texto da avaliação, que sai das listas; a nota e o resto da entrada ficam. Quem ouve
     * o evento apaga as curtidas.
     */
    @Transactional
    public void removeReviewText(long entryId) {
        LibraryEntry entry =
                entries.findById(entryId).orElseThrow(() -> new NotFoundException("Avaliação não encontrada."));
        LibraryEntryChanged.State before = entry.state();
        entry.removeReviewText();
        entries.saveAndFlush(entry);
        events.publishEvent(
                new LibraryEntryChanged(entry.getUserId(), entry.getGameId(), entry.getId(), before, entry.state()));
    }

    /** Os favoritos em destaque, na ordem escolhida. */
    @Transactional(readOnly = true)
    public List<GameSummaryDTO> featured(long userId) {
        return queries.featured(userId);
    }

    /** Até 5 favoritos em destaque, na ordem recebida (RF38); quem não veio sai do destaque. */
    @Transactional
    public List<GameSummaryDTO> setFeatured(long userId, List<Long> gameIds) {
        if (new HashSet<>(gameIds).size() < gameIds.size()) {
            throw new UnprocessableException("DUPLICATE_GAME", "Cada jogo entra uma vez no destaque.", List.of());
        }
        entries.clearFeatured(userId);
        List<FieldIssue> issues = new ArrayList<>();
        for (int i = 0; i < gameIds.size(); i++) {
            if (entries.feature(userId, gameIds.get(i), (short) (i + 1)) == 0) {
                issues.add(new FieldIssue("gameIds[" + i + "]", "não é um favorito seu"));
            }
        }
        if (!issues.isEmpty()) {
            throw new UnprocessableException("NOT_A_FAVORITE", "Só favoritos podem ficar em destaque.", issues);
        }
        return featured(userId);
    }

    @Transactional(readOnly = true)
    public LibraryCounts counts(long userId) {
        return queries.counts(userId);
    }

    @Transactional(readOnly = true)
    public LibraryEntryDTO get(long userId, long gameId) {
        return queries.findOne(userId, gameId).orElseThrow(LibraryService::notInLibrary);
    }

    /** Cria ou substitui a entrada inteira; como a chave é usuário + jogo, nunca duplica (RN01). */
    @Transactional
    public Saved put(long userId, long gameId, LibraryEntryRequest request) {
        if (!games.exists(gameId)) {
            throw new NotFoundException("Jogo " + gameId + " não encontrado.");
        }
        Optional<LibraryEntry> existing = entries.findByUserIdAndGameId(userId, gameId);
        save(existing.orElseGet(() -> new LibraryEntry(userId, gameId)), request);
        return new Saved(get(userId, gameId), existing.isEmpty());
    }

    /** JSON Merge Patch sobre a entrada atual; o resultado passa pelas mesmas regras do PUT. */
    @Transactional
    public LibraryEntryDTO patch(long userId, long gameId, JsonNode patch) {
        LibraryEntryRequest merged = mergePatch.apply(LibraryEntryRequest.of(get(userId, gameId)), patch);
        save(entries.findByUserIdAndGameId(userId, gameId).orElseThrow(), merged);
        return get(userId, gameId);
    }

    @Transactional
    public void delete(long userId, long gameId) {
        LibraryEntry entry = entries.findByUserIdAndGameId(userId, gameId).orElseThrow(LibraryService::notInLibrary);
        entries.delete(entry);
        events.publishEvent(new LibraryEntryChanged(userId, gameId, entry.getId(), entry.state(), null));
    }

    private void save(LibraryEntry entry, LibraryEntryRequest request) {
        EntryRules.check(request);
        checkReferences(request);
        LibraryEntryChanged.State before = entry.getId() == null ? null : entry.state();
        entry.replace(request);
        try {
            entries.saveAndFlush(entry);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("A entrada mudou ao mesmo tempo em outra requisição. Tente de novo.");
        }
        events.publishEvent(
                new LibraryEntryChanged(entry.getUserId(), entry.getGameId(), entry.getId(), before, entry.state()));
    }

    /** Plataforma e loja precisam existir no catálogo (RN07: qualquer plataforma, não só as do jogo). */
    private void checkReferences(LibraryEntryRequest request) {
        List<FieldIssue> issues = new ArrayList<>();
        Long platformId =
                request.playthrough() == null ? null : request.playthrough().platformId();
        if (platformId != null && !lookups.platformExists(platformId)) {
            issues.add(new FieldIssue("playthrough.platformId", "plataforma não encontrada"));
        }
        Long storeId =
                request.acquisition() == null ? null : request.acquisition().storeId();
        if (storeId != null && !lookups.storeExists(storeId)) {
            issues.add(new FieldIssue("acquisition.storeId", "loja não encontrada"));
        }
        if (!issues.isEmpty()) {
            throw new UnprocessableException("UNKNOWN_REFERENCE", "Confira os campos indicados.", issues);
        }
    }

    private static NotFoundException notInLibrary() {
        return new NotFoundException("Esse jogo não está na biblioteca.");
    }
}
