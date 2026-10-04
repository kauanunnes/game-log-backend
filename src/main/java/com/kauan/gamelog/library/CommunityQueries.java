package com.kauan.gamelog.library;

import com.kauan.gamelog.catalog.GameCommunity;
import com.kauan.gamelog.catalog.dto.CommunityDTO;
import com.kauan.gamelog.catalog.dto.CommunityDTO.RatingCount;
import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.library.dto.PublicReviewDTO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * O que a comunidade diz de um jogo. Os números e as listas de avaliações consideram só perfis públicos (RN11): com
 * poucos usuários, incluir os privados poderia expor a nota de alguém. As avaliações de uma pessoa ficam com quem
 * chama, que confere o perfil. Os números vêm prontos da tabela {@code game_community}, que gatilhos no banco refazem
 * a cada escrita (migration V13).
 */
@Repository
class CommunityQueries implements GameCommunity {
    /** Um jogo que ninguém de perfil público pôs na biblioteca. */
    private static final CommunityDTO NOBODY = new CommunityDTO(null, 0, distribution(half -> 0), null, 0, 0);

    private static final String REVIEWS = """
            SELECT e.id, u.username, u.display_name, e.status, e.rating, e.recommends, e.review_text,
                   e.has_spoilers, e.reviewed_at, g.id AS game_id, g.slug, g.title, g.cover_image_id, g.release_date,
                   (SELECT count(*) FROM review_likes l WHERE l.entry_id = e.id) AS likes
            """;

    private static final String REVIEWS_FROM = """
             FROM library_entries e
             JOIN users u ON u.id = e.user_id
             JOIN games g ON g.id = e.game_id
             WHERE e.review_text IS NOT NULL
            """;

    private final JdbcClient jdbc;

    CommunityQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public CommunityDTO of(long gameId) {
        return jdbc.sql("SELECT * FROM game_community WHERE game_id = :gameId")
                .param("gameId", gameId)
                .query((rs, row) -> {
                    Integer[] counts = (Integer[]) rs.getArray("rating_counts").getArray();
                    return new CommunityDTO(
                            rs.getBigDecimal("average_rating"),
                            rs.getLong("ratings_count"),
                            distribution(half -> counts[half]),
                            rs.getObject("recommend_percent", Integer.class),
                            rs.getLong("players_count"),
                            rs.getLong("want_to_play_count"));
                })
                .optional()
                .orElse(NOBODY);
    }

    private static List<RatingCount> distribution(IntUnaryOperator countOfHalf) {
        return IntStream.rangeClosed(0, 10)
                .mapToObj(half -> new RatingCount(half / 2.0, countOfHalf.applyAsInt(half)))
                .toList();
    }

    /** @param gameId {@code null} para as avaliações do site todo */
    Page<PublicReviewDTO> reviews(Long gameId, ReviewSort sort, Pageable pageable) {
        String where = " AND u.profile_visibility = 'PUBLIC'" + (gameId == null ? "" : " AND e.game_id = :gameId");
        Map<String, Object> params = gameId == null ? Map.of() : Map.of("gameId", gameId);
        return reviewPage(where, params, sort, pageable);
    }

    /** Das editadas por último; quem chama confere se o perfil pode ser visto. */
    Page<PublicReviewDTO> reviewsBy(long userId, Pageable pageable) {
        return reviewPage(" AND e.user_id = :userId", Map.of("userId", userId), ReviewSort.RECENT, pageable);
    }

    private Page<PublicReviewDTO> reviewPage(
            String where, Map<String, Object> params, ReviewSort sort, Pageable pageable) {
        long total = jdbc.sql("SELECT count(*)" + REVIEWS_FROM + where)
                .params(params)
                .query(Long.class)
                .single();
        List<PublicReviewDTO> content = jdbc.sql(
                        REVIEWS + REVIEWS_FROM + where + " ORDER BY " + sort.orderBy() + " LIMIT :limit OFFSET :offset")
                .params(params)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .query(CommunityQueries::review)
                .list();
        return new PageImpl<>(content, pageable, total);
    }

    /** As avaliações destas entradas, de qualquer perfil: é a moderação que pergunta. */
    List<PublicReviewDTO> reviewsByIds(Collection<Long> entryIds) {
        if (entryIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(REVIEWS + REVIEWS_FROM + " AND e.id IN (:entryIds)")
                .param("entryIds", entryIds)
                .query(CommunityQueries::review)
                .list();
    }

    private static PublicReviewDTO review(ResultSet rs, int row) throws SQLException {
        return new PublicReviewDTO(
                rs.getLong("id"),
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
                rs.getObject("reviewed_at", OffsetDateTime.class).toInstant(),
                rs.getLong("likes"));
    }
}
