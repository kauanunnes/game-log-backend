package com.kauan.gamelog.library;

import com.kauan.gamelog.catalog.GameCommunity;
import com.kauan.gamelog.catalog.dto.CommunityDTO;
import com.kauan.gamelog.catalog.dto.CommunityDTO.RatingCount;
import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.library.dto.PublicReviewDTO;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * O que a comunidade diz de um jogo. Tudo aqui considera só perfis públicos (RN11): com poucos usuários, incluir os
 * privados poderia expor a nota de alguém.
 */
@Repository
class CommunityQueries implements GameCommunity {
    private static final String PUBLIC_ENTRIES =
            " FROM library_entries e JOIN users u ON u.id = e.user_id WHERE u.profile_visibility = 'PUBLIC'";

    private static final String REVIEWS = """
            SELECT u.username, u.display_name, e.status, e.rating, e.recommends, e.review_text, e.has_spoilers,
                   e.reviewed_at, g.id AS game_id, g.slug, g.title, g.cover_image_id, g.release_date
            FROM library_entries e
            JOIN users u ON u.id = e.user_id
            JOIN games g ON g.id = e.game_id
            WHERE e.review_text IS NOT NULL AND u.profile_visibility = 'PUBLIC'
            """;

    private final JdbcClient jdbc;

    CommunityQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public CommunityDTO of(long gameId) {
        Map<Double, Long> ratings = new HashMap<>();
        jdbc.sql("SELECT floor(e.rating * 2) / 2 AS stars, count(*) AS total" + PUBLIC_ENTRIES
                        + " AND e.game_id = :gameId AND e.rating IS NOT NULL GROUP BY 1")
                .param("gameId", gameId)
                .query((RowCallbackHandler) rs -> ratings.put(rs.getDouble("stars"), rs.getLong("total")));
        List<RatingCount> distribution = IntStream.rangeClosed(0, 10)
                .mapToObj(half -> new RatingCount(half / 2.0, ratings.getOrDefault(half / 2.0, 0L)))
                .toList();

        return jdbc.sql("""
                        SELECT round(avg(e.rating), 2) AS average,
                               count(e.rating) AS ratings,
                               count(e.recommends) AS answered,
                               count(*) FILTER (WHERE e.recommends) AS recommend,
                               count(*) FILTER (WHERE e.status IN ('PLAYING', 'PLAYED', 'DROPPED')) AS players,
                               count(*) FILTER (WHERE e.status IN ('BACKLOG', 'WISHLIST')) AS want_to_play
                        """ + PUBLIC_ENTRIES + " AND e.game_id = :gameId")
                .param("gameId", gameId)
                .query((rs, row) -> {
                    long answered = rs.getLong("answered");
                    return new CommunityDTO(
                            rs.getBigDecimal("average"),
                            rs.getLong("ratings"),
                            distribution,
                            answered == 0 ? null : Math.round(rs.getLong("recommend") * 100f / answered),
                            rs.getLong("players"),
                            rs.getLong("want_to_play"));
                })
                .single();
    }

    /** @param gameId {@code null} para as avaliações do site todo */
    Page<PublicReviewDTO> reviews(Long gameId, Pageable pageable) {
        String where = gameId == null ? "" : " AND e.game_id = :gameId";
        Map<String, Object> params = gameId == null ? Map.of() : Map.of("gameId", gameId);
        long total = jdbc.sql("SELECT count(*)" + PUBLIC_ENTRIES + " AND e.review_text IS NOT NULL" + where)
                .params(params)
                .query(Long.class)
                .single();
        List<PublicReviewDTO> content = jdbc.sql(
                        REVIEWS + where + " ORDER BY e.reviewed_at DESC, e.id DESC LIMIT :limit OFFSET :offset")
                .params(params)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .query((rs, row) -> new PublicReviewDTO(
                        new PublicReviewDTO.Reviewer(rs.getString("username"), rs.getString("display_name")),
                        GameSummaryDTO.of(
                                rs.getLong("game_id"),
                                rs.getString("slug"),
                                rs.getString("title"),
                                rs.getString("cover_image_id"),
                                rs.getObject("release_date", LocalDate.class)),
                        EntryStatus.valueOf(rs.getString("status")),
                        rs.getBigDecimal("rating"),
                        rs.getObject("recommends", Boolean.class),
                        rs.getString("review_text"),
                        Boolean.TRUE.equals(rs.getObject("has_spoilers", Boolean.class)),
                        rs.getObject("reviewed_at", OffsetDateTime.class).toInstant()))
                .list();
        return new PageImpl<>(content, pageable, total);
    }
}
