package com.kauan.gamelog.library;

import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.library.dto.AcquisitionDTO;
import com.kauan.gamelog.library.dto.LibraryCounts;
import com.kauan.gamelog.library.dto.LibraryEntryDTO;
import com.kauan.gamelog.library.dto.LibraryFilter;
import com.kauan.gamelog.library.dto.MoneyDTO;
import com.kauan.gamelog.library.dto.PlaythroughDTO;
import com.kauan.gamelog.library.dto.ReviewDTO;
import com.kauan.gamelog.library.dto.TasteSignal;
import com.kauan.gamelog.shared.TextNormalizer;
import com.kauan.gamelog.shared.UnprocessableException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Leituras da biblioteca em SQL, já com o resumo do jogo; as escritas passam pela entidade. */
@Repository
class LibraryQueries {
    private static final Map<String, String> SORTABLE = Map.of(
            "createdAt", "e.created_at",
            "updatedAt", "e.updated_at",
            "rating", "e.rating",
            "title", "g.title",
            "finishedOn", "e.finished_on");

    private static final String SELECT = """
            SELECT e.status, e.favorite, e.rating, e.recommends, e.review_text, e.has_spoilers,
                   e.played_platform_id, e.hours_played, e.started_on, e.finished_on, e.completed,
                   e.acquisition, e.store_id, e.price_paid, e.currency, e.acquired_on, e.created_at, e.updated_at,
                   g.id AS game_id, g.slug, g.title, g.cover_image_id, g.release_date
            FROM library_entries e JOIN games g ON g.id = e.game_id
            """;

    private final JdbcClient jdbc;

    LibraryQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Optional<LibraryEntryDTO> findOne(long userId, long gameId) {
        return jdbc.sql(SELECT + " WHERE e.user_id = :userId AND e.game_id = :gameId")
                .param("userId", userId)
                .param("gameId", gameId)
                .query(LibraryQueries::map)
                .optional();
    }

    Page<LibraryEntryDTO> find(long userId, LibraryFilter filter, Pageable pageable) {
        List<String> conditions = new ArrayList<>(List.of("e.user_id = :userId"));
        Map<String, Object> params = new HashMap<>(Map.of("userId", userId));

        if (filter.status() != null && !filter.status().isEmpty()) {
            conditions.add("e.status IN (:status)");
            params.put("status", filter.status().stream().map(Enum::name).toList());
        }
        if (filter.favorite() != null) {
            conditions.add("e.favorite = :favorite");
            params.put("favorite", filter.favorite());
        }
        if (filter.genreId() != null) {
            conditions.add(
                    "EXISTS (SELECT 1 FROM game_genres gg WHERE gg.game_id = e.game_id AND gg.genre_id = :genreId)");
            params.put("genreId", filter.genreId());
        }
        if (filter.platformId() != null) {
            conditions.add("e.played_platform_id = :platformId");
            params.put("platformId", filter.platformId());
        }
        if (filter.minRating() != null) {
            conditions.add("e.rating >= :minRating");
            params.put("minRating", filter.minRating());
        }
        if (filter.recommends() != null) {
            conditions.add("e.recommends = :recommends");
            params.put("recommends", filter.recommends());
        }
        if (filter.completed() != null) {
            conditions.add("coalesce(e.completed, false) = :completed");
            params.put("completed", filter.completed());
        }
        if (filter.reviewed() != null) {
            conditions.add(filter.reviewed() ? "e.review_text IS NOT NULL" : "e.review_text IS NULL");
        }
        if (filter.q() != null && !filter.q().isBlank()) {
            conditions.add("strpos(g.title_normalized, :q) > 0");
            params.put("q", TextNormalizer.normalize(filter.q()));
        }
        return page(conditions, params, orderBy(pageable.getSort()), pageable);
    }

    /** Os favoritos em destaque, na ordem escolhida (RF38). */
    List<GameSummaryDTO> featured(long userId) {
        return jdbc.sql("""
                        SELECT g.id, g.slug, g.title, g.cover_image_id, g.release_date
                        FROM library_entries e JOIN games g ON g.id = e.game_id
                        WHERE e.user_id = :userId AND e.favorite_position IS NOT NULL
                        ORDER BY e.favorite_position
                        """)
                .param("userId", userId)
                .query((rs, row) -> GameSummaryDTO.of(
                        rs.getLong("id"),
                        rs.getString("slug"),
                        rs.getString("title"),
                        rs.getString("cover_image_id"),
                        rs.getObject("release_date", LocalDate.class)))
                .list();
    }

    List<TasteSignal> taste(long userId) {
        return jdbc.sql("""
                        SELECT game_id, status, favorite, rating, recommends, updated_at
                        FROM library_entries WHERE user_id = :userId
                        """)
                .param("userId", userId)
                .query((rs, row) -> new TasteSignal(
                        rs.getLong("game_id"),
                        EntryStatus.valueOf(rs.getString("status")),
                        rs.getBoolean("favorite"),
                        rs.getBigDecimal("rating"),
                        rs.getObject("recommends", Boolean.class),
                        rs.getObject("updated_at", OffsetDateTime.class).toInstant()))
                .list();
    }

    LibraryCounts counts(long userId) {
        return jdbc.sql("""
                        SELECT count(*) FILTER (WHERE status = 'PLAYED') AS played,
                               count(*) FILTER (WHERE status = 'PLAYING') AS playing,
                               count(*) FILTER (WHERE status = 'BACKLOG') AS backlog,
                               count(*) FILTER (WHERE status = 'WISHLIST') AS wishlist,
                               count(*) FILTER (WHERE status = 'DROPPED') AS dropped,
                               count(*) FILTER (WHERE favorite) AS favorites,
                               count(*) FILTER (WHERE review_text IS NOT NULL) AS reviews
                        FROM library_entries WHERE user_id = :userId
                        """)
                .param("userId", userId)
                .query((rs, row) -> new LibraryCounts(
                        rs.getLong("played"),
                        rs.getLong("playing"),
                        rs.getLong("backlog"),
                        rs.getLong("wishlist"),
                        rs.getLong("dropped"),
                        rs.getLong("favorites"),
                        rs.getLong("reviews")))
                .single();
    }

    private Page<LibraryEntryDTO> page(
            List<String> conditions, Map<String, Object> params, String orderBy, Pageable pageable) {
        String where = " WHERE " + String.join(" AND ", conditions);
        long total = jdbc.sql("SELECT count(*) FROM library_entries e JOIN games g ON g.id = e.game_id" + where)
                .params(params)
                .query(Long.class)
                .single();
        List<LibraryEntryDTO> content = jdbc.sql(
                        SELECT + where + " ORDER BY " + orderBy + " LIMIT :limit OFFSET :offset")
                .params(params)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .query(LibraryQueries::map)
                .list();
        return new PageImpl<>(content, pageable, total);
    }

    /** Só campos conhecidos entram no SQL; sem ordenação, os alterados por último vêm primeiro. */
    private static String orderBy(Sort sort) {
        if (sort.isUnsorted()) {
            return "e.updated_at DESC, e.id DESC";
        }
        String columns = sort.stream()
                .map(order -> {
                    String column = SORTABLE.get(order.getProperty());
                    if (column == null) {
                        throw UnprocessableException.field(
                                "INVALID_SORT", "sort", "ordene por " + String.join(", ", SORTABLE.keySet()));
                    }
                    return column + (order.isAscending() ? " ASC" : " DESC") + " NULLS LAST";
                })
                .collect(Collectors.joining(", "));
        return columns + ", e.id DESC";
    }

    private static LibraryEntryDTO map(ResultSet rs, int row) throws SQLException {
        GameSummaryDTO game = GameSummaryDTO.of(
                rs.getLong("game_id"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("cover_image_id"),
                rs.getObject("release_date", LocalDate.class));
        ReviewDTO review = new ReviewDTO(
                rs.getBigDecimal("rating"),
                rs.getObject("recommends", Boolean.class),
                rs.getString("review_text"),
                rs.getObject("has_spoilers", Boolean.class));
        PlaythroughDTO playthrough = new PlaythroughDTO(
                rs.getObject("played_platform_id", Long.class),
                rs.getObject("hours_played", Integer.class),
                rs.getObject("started_on", LocalDate.class),
                rs.getObject("finished_on", LocalDate.class),
                rs.getObject("completed", Boolean.class));
        String method = rs.getString("acquisition");
        BigDecimal price = rs.getBigDecimal("price_paid");
        AcquisitionDTO acquisition = new AcquisitionDTO(
                method == null ? null : AcquisitionMethod.valueOf(method),
                rs.getObject("store_id", Long.class),
                price == null ? null : new MoneyDTO(price, rs.getString("currency")),
                rs.getObject("acquired_on", LocalDate.class));
        return new LibraryEntryDTO(
                game,
                EntryStatus.valueOf(rs.getString("status")),
                rs.getBoolean("favorite"),
                review.blank() ? null : review,
                playthrough.blank() ? null : playthrough,
                acquisition.blank() ? null : acquisition,
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant());
    }
}
