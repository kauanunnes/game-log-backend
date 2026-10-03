package com.kauan.gamelog.catalog.igdb;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Importação inicial dos jogos mais populares do IGDB. Só roda quando pedida:
 * {@code --game-log.igdb.bootstrap-limit=2000}.
 */
@Component
class IgdbBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(IgdbBootstrap.class);
    private static final int PAGE_SIZE = 500;

    private final IgdbProperties properties;
    private final IgdbClient client;
    private final IgdbImporter importer;

    IgdbBootstrap(IgdbProperties properties, IgdbClient client, IgdbImporter importer) {
        this.properties = properties;
        this.client = client;
        this.importer = importer;
    }

    @Override
    public void run(ApplicationArguments args) {
        int limit = properties.bootstrapLimit();
        if (limit <= 0) {
            return;
        }
        if (!properties.enabled()) {
            log.warn("IGDB sem credenciais: a importação inicial não vai rodar");
            return;
        }
        int imported = 0;
        try {
            for (int offset = 0; offset < limit; offset += PAGE_SIZE) {
                List<IgdbGame> page = client.popular(Math.min(PAGE_SIZE, limit - offset), offset);
                imported += importer.importGames(page);
                log.info("IGDB: {} jogos importados até agora", imported);
                if (page.size() < PAGE_SIZE) {
                    break;
                }
            }
        } catch (RuntimeException e) {
            // A aplicação sobe mesmo assim. Rodar de novo é seguro: a importação atualiza o que já existe.
            log.error("A importação inicial do IGDB parou depois de {} jogos", imported, e);
        }
    }
}
