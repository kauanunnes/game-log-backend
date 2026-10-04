package com.kauan.gamelog.catalog.igdb;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Atualiza os jogos sincronizados com o IGDB há mais de uma semana: notas, capas e metadados mudam com o tempo. Roda
 * depois da preparação do catálogo na subida e toda segunda às 5h UTC ({@code game-log.igdb.resync-cron}). Numa
 * hospedagem que hiberna, a rodada da subida faz o papel do agendamento.
 */
@Component
class IgdbResync {
    private static final Logger log = LoggerFactory.getLogger(IgdbResync.class);
    private static final int STALE_AFTER_DAYS = 7;
    /** O máximo de jogos por chamada ao IGDB. */
    private static final int BATCH = 500;

    private final IgdbProperties properties;
    private final IgdbClient client;
    private final IgdbImporter importer;
    private final AtomicBoolean running = new AtomicBoolean();

    IgdbResync(IgdbProperties properties, IgdbClient client, IgdbImporter importer) {
        this.properties = properties;
        this.client = client;
        this.importer = importer;
    }

    /** Uma rodada de cada vez; se outra já está rodando, esta não faz nada. */
    @Scheduled(cron = "${game-log.igdb.resync-cron:0 0 5 * * MON}", zone = "UTC")
    void resyncStale() {
        if (!properties.enabled() || !running.compareAndSet(false, true)) {
            return;
        }
        int updated = 0;
        try {
            List<Long> ids;
            while (!(ids = importer.staleIgdbIds(STALE_AFTER_DAYS, BATCH)).isEmpty()) {
                for (IgdbGame game : client.findByIds(ids)) {
                    if (importer.importGame(game).isPresent()) {
                        updated++;
                    }
                }
                // Inclusive os que o IGDB não devolveu, para não voltarem na mesma rodada
                importer.markSynced(ids);
            }
        } catch (RuntimeException e) {
            log.error("A atualização dos jogos do IGDB parou depois de {} jogos", updated, e);
        } finally {
            running.set(false);
        }
        if (updated > 0) {
            log.info("IGDB: {} jogos atualizados", updated);
        }
    }
}
