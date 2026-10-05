package com.kauan.gamelog.recommendation;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kauan.gamelog.catalog.GameService;
import com.kauan.gamelog.library.LibraryEntryChanged;
import com.kauan.gamelog.recommendation.dto.SuggestionDTO;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * A curadoria do Claude roda em segundo plano, porque a chamada leva alguns segundos: enquanto isso, a tela mostra a
 * busca e pergunta de novo. O resultado vale por 24 h ou até a biblioteca mudar; depois de uma falha, a próxima tentativa
 * espera 10 minutos. Uma pessoa tem no máximo uma curadoria rodando.
 */
@Component
class Curations {
    private static final Logger log = LoggerFactory.getLogger(Curations.class);

    private final Curator curator;
    private final GameService games;
    private final Cache<Long, List<SuggestionDTO>> curated = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(24))
            .maximumSize(10_000)
            .build();
    private final Cache<Long, Boolean> failed = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(10_000)
            .build();
    /** Cada mudança na biblioteca avança a versão da pessoa; o resultado de uma versão antiga é descartado. */
    private final Map<Long, Long> versions = new ConcurrentHashMap<>();

    private final Set<Long> running = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    Curations(Curator curator, GameService games) {
        this.curator = curator;
        this.games = games;
    }

    /** @return as sugestões escolhidas pelo Claude, ou {@code null} se ainda não há */
    List<SuggestionDTO> curated(long userId) {
        return curated.getIfPresent(userId);
    }

    /** @return se há uma curadoria em andamento para a pessoa */
    boolean start(long userId, Curator.Input input) {
        if (!curator.enabled() || failed.getIfPresent(userId) != null) {
            return false;
        }
        if (!running.add(userId)) {
            return true;
        }
        long version = versions.getOrDefault(userId, 0L);
        executor.execute(() -> {
            try {
                List<Curator.Pick> picks = curator.curate(input);
                if (versions.getOrDefault(userId, 0L) != version) {
                    return;
                }
                if (picks.isEmpty()) {
                    failed.put(userId, true);
                } else {
                    curated.put(userId, suggestions(picks));
                }
            } catch (RuntimeException | LinkageError e) {
                // Uma dependência quebrada também não pode repetir a chamada a cada pedido
                log.warn("A curadoria das sugestões falhou", e);
                failed.put(userId, true);
            } finally {
                running.remove(userId);
            }
        });
        return true;
    }

    @TransactionalEventListener
    void on(LibraryEntryChanged event) {
        versions.merge(event.userId(), 1L, Long::sum);
        curated.invalidate(event.userId());
        failed.invalidate(event.userId());
    }

    @PreDestroy
    void stop() {
        executor.shutdownNow();
    }

    private List<SuggestionDTO> suggestions(List<Curator.Pick> picks) {
        Map<Long, String> reasons = new LinkedHashMap<>();
        picks.forEach(pick -> reasons.putIfAbsent(pick.gameId(), pick.reason()));
        return games.summaries(List.copyOf(reasons.keySet())).stream()
                .map(game -> new SuggestionDTO(game, reasons.get(game.id())))
                .toList();
    }
}
