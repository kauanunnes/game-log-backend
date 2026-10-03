package com.kauan.gamelog.library;

import com.kauan.gamelog.catalog.GameService;
import com.kauan.gamelog.catalog.LookupService;
import com.kauan.gamelog.library.dto.LibraryCounts;
import com.kauan.gamelog.library.dto.LibraryEntryDTO;
import com.kauan.gamelog.library.dto.LibraryEntryRequest;
import com.kauan.gamelog.library.dto.LibraryFilter;
import com.kauan.gamelog.library.dto.StatsDTO;
import com.kauan.gamelog.shared.ConflictException;
import com.kauan.gamelog.shared.FieldIssue;
import com.kauan.gamelog.shared.JsonMergePatch;
import com.kauan.gamelog.shared.NotFoundException;
import com.kauan.gamelog.shared.UnprocessableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
public class LibraryService {
    private final LibraryEntryRepository entries;
    private final LibraryQueries queries;
    private final LibraryStats stats;
    private final GameService games;
    private final LookupService lookups;
    private final JsonMergePatch mergePatch;
    private final ApplicationEventPublisher events;

    LibraryService(
            LibraryEntryRepository entries,
            LibraryQueries queries,
            LibraryStats stats,
            GameService games,
            LookupService lookups,
            JsonMergePatch mergePatch,
            ApplicationEventPublisher events) {
        this.entries = entries;
        this.queries = queries;
        this.stats = stats;
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

    /** Entradas com texto de avaliação, das editadas por último. */
    @Transactional(readOnly = true)
    public Page<LibraryEntryDTO> reviews(long userId, Pageable pageable) {
        return queries.findReviews(userId, pageable);
    }

    /** Estatísticas completas; quem expõe numa rota pública decide se mostra os gastos. */
    @Transactional(readOnly = true)
    public StatsDTO stats(long userId, Integer year) {
        return stats.of(userId, year);
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
        events.publishEvent(new LibraryEntryChanged(userId, gameId, null));
    }

    private void save(LibraryEntry entry, LibraryEntryRequest request) {
        EntryRules.check(request);
        checkReferences(request);
        entry.replace(request);
        try {
            entries.saveAndFlush(entry);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("A entrada mudou ao mesmo tempo em outra requisição. Tente de novo.");
        }
        events.publishEvent(new LibraryEntryChanged(entry.getUserId(), entry.getGameId(), entry.getStatus()));
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
