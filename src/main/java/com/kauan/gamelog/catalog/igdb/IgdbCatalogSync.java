package com.kauan.gamelog.catalog.igdb;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Traz jogos do IGDB para o catálogo local: quando uma busca acha pouca coisa ou pelo id do IGDB. */
@Component
public class IgdbCatalogSync {
    private static final Logger log = LoggerFactory.getLogger(IgdbCatalogSync.class);
    private static final Duration SEARCH_COOLDOWN = Duration.ofHours(24);
    private static final int SEARCH_LIMIT = 10;
    private static final int MAX_REMEMBERED_SEARCHES = 10_000;

    private final IgdbProperties properties;
    private final IgdbClient client;
    private final IgdbImporter importer;
    private final Map<String, Instant> recentSearches = new ConcurrentHashMap<>();

    IgdbCatalogSync(IgdbProperties properties, IgdbClient client, IgdbImporter importer) {
        this.properties = properties;
        this.client = client;
        this.importer = importer;
    }

    /**
     * Busca no IGDB e importa o que encontrar. A mesma busca só vai ao IGDB uma vez a cada 24 h; se o
     * IGDB falhar, registra no log e segue só com o catálogo local.
     *
     * @return se algum jogo foi importado
     */
    public boolean importSearch(String query) {
        if (!properties.enabled() || searchedRecently(query)) {
            return false;
        }
        try {
            return importer.importGames(client.search(query, SEARCH_LIMIT)) > 0;
        } catch (RuntimeException e) {
            recentSearches.remove(query);
            log.warn("Busca no IGDB falhou para \"{}\": {}", query, e.getMessage());
            return false;
        }
    }

    /**
     * Importa ou atualiza um jogo pelo id do IGDB (RF26). Erros do IGDB sobem para quem chamou.
     *
     * @return o id local do jogo, ou vazio se o IGDB não o conhece ou se o tipo dele fica fora do catálogo
     */
    public Optional<Long> importById(long igdbId) {
        return client.findById(igdbId).flatMap(importer::importGame);
    }

    /** Marca a busca como feita; com putIfAbsent e replace, duas buscas iguais ao mesmo tempo vão ao IGDB uma vez só. */
    private boolean searchedRecently(String query) {
        if (recentSearches.size() >= MAX_REMEMBERED_SEARCHES) {
            recentSearches.clear();
        }
        Instant now = Instant.now();
        Instant last = recentSearches.putIfAbsent(query, now);
        if (last == null) {
            return false;
        }
        return last.isAfter(now.minus(SEARCH_COOLDOWN)) || !recentSearches.replace(query, last, now);
    }
}
