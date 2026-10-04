package com.kauan.gamelog.catalog.igdb;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Na subida, avisa no log se o IGDB está ligado e prepara o catálogo numa thread à parte, para a aplicação já atender:
 * importa os jogos mais populares, se pedido ({@code --game-log.igdb.bootstrap-limit=10000}), e atualiza os antigos.
 */
@Component
class IgdbBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(IgdbBootstrap.class);
    private static final int PAGE_SIZE = 500;

    private final IgdbProperties properties;
    private final IgdbClient client;
    private final IgdbImporter importer;
    private final IgdbResync resync;

    IgdbBootstrap(IgdbProperties properties, IgdbClient client, IgdbImporter importer, IgdbResync resync) {
        this.properties = properties;
        this.client = client;
        this.importer = importer;
        this.resync = resync;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            log.warn("IGDB desligado: sem IGDB_CLIENT_ID e IGDB_CLIENT_SECRET, a busca usa só o banco");
            return;
        }
        try {
            client.checkCredentials();
        } catch (RuntimeException e) {
            log.error(
                    "IGDB fora: a Twitch não liberou o token ({}). Confira IGDB_CLIENT_ID e IGDB_CLIENT_SECRET",
                    e.getMessage());
            return;
        }
        log.info("IGDB ligado");
        Thread.ofVirtual().name("igdb-bootstrap").start(this::prepareCatalog);
    }

    void prepareCatalog() {
        if (properties.bootstrapLimit() > 0) {
            importPopular(properties.bootstrapLimit());
        }
        resync.resyncStale();
    }

    /** Com o catálogo já desse tamanho, não roda: a variável pode ficar ligada em produção. */
    private void importPopular(int limit) {
        long existing = importer.importedCount();
        if (existing >= limit) {
            log.info("IGDB: o catálogo já tem {} jogos do IGDB, a importação inicial não precisa rodar", existing);
            return;
        }
        int imported = 0;
        try {
            for (int offset = 0; offset < limit; offset += PAGE_SIZE) {
                List<IgdbGame> page = client.popular(Math.min(PAGE_SIZE, limit - offset), offset);
                // Um jogo por transação, para quem está usando o site não esperar uma página inteira
                for (IgdbGame game : page) {
                    if (importer.importGame(game).isPresent()) {
                        imported++;
                    }
                }
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
