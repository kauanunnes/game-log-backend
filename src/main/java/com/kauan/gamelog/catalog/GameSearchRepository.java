package com.kauan.gamelog.catalog;

import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class GameSearchRepository {
    private final JdbcClient jdbc;

    public GameSearchRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Espera um {@link GameSearch} já normalizado. */
    @Transactional(readOnly = true)
    public Page<GameSummaryDTO> search(GameSearch search, Pageable pageable) {
        List<String> filters = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (search.q() != null) {
            // O padrão (0.6) recusa erros de digitação como "wicher"; vale só para esta transação.
            jdbc.sql("SET LOCAL pg_trgm.word_similarity_threshold = 0.4").update();
            filters.add("(:q <% g.title_normalized OR g.title_normalized LIKE '%' || :q || '%')");
            params.put("q", search.q());
        }
        if (search.genreId() != null) {
            filters.add("EXISTS (SELECT 1 FROM game_genres gg WHERE gg.game_id = g.id AND gg.genre_id = :genreId)");
            params.put("genreId", search.genreId());
        }
        if (search.platformId() != null) {
            filters.add(
                    "EXISTS (SELECT 1 FROM game_platforms gp WHERE gp.game_id = g.id AND gp.platform_id = :platformId)");
            params.put("platformId", search.platformId());
        }
        if (search.year() != null) {
            filters.add("g.release_date >= :from AND g.release_date < :to");
            params.put("from", LocalDate.of(search.year(), 1, 1));
            params.put("to", LocalDate.of(search.year() + 1, 1, 1));
        }
        String where = filters.isEmpty() ? "" : " WHERE " + String.join(" AND ", filters);

        long total = jdbc.sql("SELECT count(*) FROM games g" + where)
                .params(params)
                .query(Long.class)
                .single();

        List<GameSummaryDTO> content = jdbc.sql("SELECT g.id, g.slug, g.title, g.cover_image_id, g.release_date"
                        + " FROM games g" + where
                        + " ORDER BY " + search.sort().orderBy()
                        + " LIMIT :limit OFFSET :offset")
                .params(params)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .query((rs, row) -> GameSummaryDTO.of(
                        rs.getLong("id"),
                        rs.getString("slug"),
                        rs.getString("title"),
                        rs.getString("cover_image_id"),
                        rs.getObject("release_date", LocalDate.class)))
                .list();

        return new PageImpl<>(content, pageable, total);
    }
}
