package com.kauan.gamelog.recommendation;

import com.kauan.gamelog.catalog.GameImported;
import com.kauan.gamelog.catalog.GameService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Indexa os jogos numa thread só, em lotes, para o cálculo não disputar a CPU com as requisições. Na subida, confere o
 * catálogo inteiro (jogos novos, textos que mudaram e vetores de outro modelo); depois, cada jogo que entra ou muda
 * pelo IGDB.
 */
@Component
class EmbeddingWorker implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(EmbeddingWorker.class);
    private static final int BATCH = 32;
    private static final int PAGE = 500;

    private final GameIndexer indexer;
    private final GameService games;
    private final BlockingQueue<Long> queue = new LinkedBlockingQueue<>();
    private volatile Thread worker;

    EmbeddingWorker(GameIndexer indexer, GameService games) {
        this.indexer = indexer;
        this.games = games;
    }

    @TransactionalEventListener
    void on(GameImported event) {
        if (indexer.enabled()) {
            queue.add(event.gameId());
        }
    }

    @Override
    public void start() {
        if (!indexer.enabled()) {
            log.info("Sem modelo de embeddings (spring.ai.model.embedding=none): os jogos não são indexados");
            return;
        }
        worker = Thread.ofVirtual().name("game-embeddings").start(this::work);
    }

    @Override
    public void stop() {
        worker.interrupt();
    }

    @Override
    public boolean isRunning() {
        return worker != null && worker.isAlive();
    }

    private void work() {
        enqueueCatalog();
        int indexed = 0;
        while (!Thread.currentThread().isInterrupted()) {
            List<Long> batch = new ArrayList<>(BATCH);
            try {
                batch.add(queue.take());
            } catch (InterruptedException e) {
                return;
            }
            queue.drainTo(batch, BATCH - 1);
            try {
                indexed += indexer.index(batch);
            } catch (RuntimeException e) {
                log.warn("Não foi possível indexar os jogos {}", batch, e);
            }
            if (queue.isEmpty() && indexed > 0) {
                log.info("Jogos indexados nos embeddings: {}", indexed);
                indexed = 0;
            }
        }
    }

    private void enqueueCatalog() {
        long last = 0;
        for (List<Long> ids = games.idsAfter(last, PAGE); !ids.isEmpty(); ids = games.idsAfter(last, PAGE)) {
            queue.addAll(ids);
            last = ids.getLast();
        }
    }
}
