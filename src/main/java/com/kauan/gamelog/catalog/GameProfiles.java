package com.kauan.gamelog.catalog;

import com.kauan.gamelog.catalog.dto.GameProfile;
import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.RowCallbackHandler;
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
                        SELECT g.id, g.igdb_id, g.title, g.release_date, g.summary, g.metadata::text AS metadata,
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
                        rs.getObject("igdb_id", Long.class),
                        rs.getString("title"),
                        rs.getObject("release_date", LocalDate.class),
                        rs.getString("summary"),
                        List.of((String[]) rs.getArray("genres").getArray()),
                        JSON.readValue(rs.getString("metadata"), GameMetadata.class)))
                .list();
    }

    /** Os cards destes jogos, na ordem dos ids; os que não existem ficam de fora. */
    List<GameSummaryDTO> summaries(List<Long> ids) {
        return inOrder(ids, "id");
    }

    /** O mesmo, pelos ids do IGDB: os jogos que não estão no catálogo ficam de fora. */
    List<GameSummaryDTO> summariesByIgdbIds(List<Long> igdbIds) {
        return inOrder(igdbIds, "igdb_id");
    }

    /** {@code column} é sempre uma constante desta classe, nunca entrada de usuário. */
    private List<GameSummaryDTO> inOrder(List<Long> keys, String column) {
        if (keys.isEmpty()) {
            return List.of();
        }
        Map<Long, GameSummaryDTO> found = new HashMap<>();
        jdbc.sql("SELECT " + column + " AS key, id, slug, title, cover_image_id, release_date FROM games WHERE "
                        + column + " IN (:keys)")
                .param("keys", keys)
                .query((RowCallbackHandler) rs -> found.put(
                        rs.getLong("key"),
                        GameSummaryDTO.of(
                                rs.getLong("id"),
                                rs.getString("slug"),
                                rs.getString("title"),
                                rs.getString("cover_image_id"),
                                rs.getObject("release_date", LocalDate.class))));
        return keys.stream().map(found::get).filter(Objects::nonNull).toList();
    }

    List<Long> idsAfter(long afterId, int limit) {
        return jdbc.sql("SELECT id FROM games WHERE id > :afterId ORDER BY id LIMIT :limit")
                .param("afterId", afterId)
                .param("limit", limit)
                .query(Long.class)
                .list();
    }
}
