package com.kauan.gamelog.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.kauan.gamelog.FakeEmbeddingModel;
import com.kauan.gamelog.IntegrationTest;
import com.kauan.gamelog.catalog.GameImported;
import com.kauan.gamelog.catalog.GameService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/** Cada teste usa um jogo do seed; a indexação da subida roda em segundo plano, então todos esperam por ela. */
@IntegrationTest
class GameIndexerTests {
    @Autowired
    private GameIndexer indexer;

    @Autowired
    private GameService games;

    @Autowired
    private FakeEmbeddingModel model;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private TransactionTemplate transaction;

    @Test
    void theWholeCatalogIsIndexedOnStartup() {
        long id = indexed("hollow-knight");

        assertThat(storedHash(id)).isEqualTo(GameIndexer.hash(text(id)));
        assertThat(jdbc.sql("SELECT vector_dims(embedding) FROM game_embeddings WHERE game_id = :id")
                        .param("id", id)
                        .query(Integer.class)
                        .single())
                .isEqualTo(384);
    }

    @Test
    void anUnchangedGameIsNotEmbeddedAgain() {
        long id = indexed("celeste");
        long times = model.timesEmbedded(text(id));

        assertThat(indexer.index(List.of(id))).isZero();
        assertThat(model.timesEmbedded(text(id))).isEqualTo(times);
    }

    @Test
    void aNewTextOrAnotherModelIsEmbeddedAgain() {
        long id = indexed("hades");

        jdbc.sql("UPDATE game_embeddings SET text_hash = repeat('0', 64) WHERE game_id = :id")
                .param("id", id)
                .update();
        assertThat(indexer.index(List.of(id))).isOne();
        assertThat(storedHash(id)).isEqualTo(GameIndexer.hash(text(id)));

        jdbc.sql("UPDATE game_embeddings SET model = 'outro-modelo' WHERE game_id = :id")
                .param("id", id)
                .update();
        assertThat(indexer.index(List.of(id))).isOne();
        assertThat(jdbc.sql("SELECT model FROM game_embeddings WHERE game_id = :id")
                        .param("id", id)
                        .query(String.class)
                        .single())
                .isEqualTo("teste");
    }

    @Test
    void anImportedGameGoesThroughTheQueue() {
        long id = indexed("stardew-valley");
        jdbc.sql("UPDATE game_embeddings SET text_hash = repeat('0', 64) WHERE game_id = :id")
                .param("id", id)
                .update();

        transaction.executeWithoutResult(status -> events.publishEvent(new GameImported(id)));

        await().untilAsserted(() -> assertThat(storedHash(id)).isEqualTo(GameIndexer.hash(text(id))));
    }

    private long indexed(String slug) {
        long id = jdbc.sql("SELECT id FROM games WHERE slug = :slug")
                .param("slug", slug)
                .query(Long.class)
                .single();
        await().untilAsserted(() -> assertThat(storedHash(id)).isNotNull());
        return id;
    }

    private String storedHash(long id) {
        return jdbc.sql("SELECT text_hash FROM game_embeddings WHERE game_id = :id")
                .param("id", id)
                .query(String.class)
                .optional()
                .orElse(null);
    }

    private String text(long id) {
        return GameText.of(games.profiles(List.of(id)).getFirst());
    }
}
