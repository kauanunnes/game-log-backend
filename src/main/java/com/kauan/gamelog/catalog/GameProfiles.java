package com.kauan.gamelog.catalog;

import com.kauan.gamelog.catalog.dto.GameProfile;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Os jogos em lote, numa consulta só, sem carregar as entidades. */
@Repository
class GameProfiles {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final JdbcClient jdbc;

    GameProfiles(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<GameProfile> of(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("""
                        SELECT g.id, g.title, g.release_date, g.summary, g.metadata::text AS metadata,
                               array_remove(array_agg(ge.name ORDER BY ge.name), NULL) AS genres
                        FROM games g
                        LEFT JOIN game_genres gg ON gg.game_id = g.id
                        LEFT JOIN genres ge ON ge.id = gg.genre_id
                        WHERE g.id IN (:ids)
                        GROUP BY g.id
                        ORDER BY g.id
                        """)
                .param("ids", ids)
                .query((rs, row) -> new GameProfile(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getObject("release_date", LocalDate.class),
                        rs.getString("summary"),
                        List.of((String[]) rs.getArray("genres").getArray()),
                        JSON.readValue(rs.getString("metadata"), GameMetadata.class)))
                .list();
    }

    List<Long> idsAfter(long afterId, int limit) {
        return jdbc.sql("SELECT id FROM games WHERE id > :afterId ORDER BY id LIMIT :limit")
                .param("afterId", afterId)
                .param("limit", limit)
                .query(Long.class)
                .list();
    }
}
