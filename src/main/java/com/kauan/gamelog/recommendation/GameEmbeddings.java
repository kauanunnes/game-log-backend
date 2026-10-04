package com.kauan.gamelog.recommendation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Os vetores dos jogos, na tabela {@code game_embeddings} (pgvector). */
@Repository
class GameEmbeddings {
    private final JdbcClient jdbc;

    GameEmbeddings(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** O hash do texto de cada jogo já indexado com este modelo; os de outro modelo ficam de fora. */
    Map<Long, String> hashes(Collection<Long> gameIds, String model) {
        Map<Long, String> hashes = new HashMap<>();
        if (gameIds.isEmpty()) {
            return hashes;
        }
        jdbc.sql("SELECT game_id, text_hash FROM game_embeddings WHERE game_id IN (:gameIds) AND model = :model")
                .param("gameIds", gameIds)
                .param("model", model)
                .query((RowCallbackHandler) rs -> hashes.put(rs.getLong("game_id"), rs.getString("text_hash")));
        return hashes;
    }

    /**
     * Os jogos de vetor mais próximo (cosseno), do mais parecido ao menos; vazio se o jogo não tem vetor. Só compara
     * vetores do mesmo modelo.
     */
    List<Long> nearest(long gameId, int limit) {
        return jdbc.sql("""
                        SELECT game_id FROM game_embeddings
                        WHERE game_id <> :gameId
                          AND model = (SELECT model FROM game_embeddings WHERE game_id = :gameId)
                        ORDER BY embedding <=> (SELECT embedding FROM game_embeddings WHERE game_id = :gameId)
                        LIMIT :limit
                        """)
                .param("gameId", gameId)
                .param("limit", limit)
                .query(Long.class)
                .list();
    }

    void save(long gameId, float[] embedding, String textHash, String model) {
        jdbc.sql("""
                        INSERT INTO game_embeddings (game_id, embedding, text_hash, model)
                        VALUES (:gameId, CAST(:embedding AS vector), :textHash, :model)
                        ON CONFLICT (game_id) DO UPDATE SET
                            embedding = EXCLUDED.embedding, text_hash = EXCLUDED.text_hash,
                            model = EXCLUDED.model, updated_at = now()
                        """)
                .param("gameId", gameId)
                .param("embedding", literal(embedding))
                .param("textHash", textHash)
                .param("model", model)
                .update();
    }

    /** O formato de texto do pgvector: {@code [0.1,0.2,...]}. */
    private static String literal(float[] embedding) {
        StringJoiner values = new StringJoiner(",", "[", "]");
        for (float value : embedding) {
            values.add(Float.toString(value));
        }
        return values.toString();
    }
}
