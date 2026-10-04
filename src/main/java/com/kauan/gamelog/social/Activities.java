package com.kauan.gamelog.social;

import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.library.EntryStatus;
import com.kauan.gamelog.library.dto.ReviewDTO;
import com.kauan.gamelog.social.dto.ActivityDTO;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** A tabela {@code activities}: o que cada pessoa fez na biblioteca, para o feed de quem a segue. */
@Repository
class Activities {
    /** Só perfis públicos (RN15) de quem eu sigo. */
    private static final String FEED = """
             FROM activities a
             JOIN follows f ON f.followee_id = a.user_id AND f.follower_id = :userId
             JOIN users u ON u.id = a.user_id AND u.profile_visibility = 'PUBLIC'
            """;

    private final JdbcClient jdbc;

    Activities(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    void addStatus(long userId, long gameId, long entryId, EntryStatus status, boolean completed) {
        jdbc.sql("""
                        INSERT INTO activities (user_id, type, game_id, entry_id, data)
                        VALUES (:userId, 'STATUS', :gameId, :entryId,
                                jsonb_build_object('status', CAST(:status AS text), 'completed', CAST(:completed AS boolean)))
                        """)
                .param("userId", userId)
                .param("gameId", gameId)
                .param("entryId", entryId)
                .param("status", status.name())
                .param("completed", completed)
                .update();
    }

    /** Uma atividade desse tipo por entrada: a nova toma o lugar da antiga e vai para o topo. */
    void replace(ActivityType type, long userId, long gameId, long entryId) {
        remove(type, entryId);
        jdbc.sql("""
                        INSERT INTO activities (user_id, type, game_id, entry_id)
                        VALUES (:userId, :type, :gameId, :entryId)
                        """)
                .param("userId", userId)
                .param("type", type.name())
                .param("gameId", gameId)
                .param("entryId", entryId)
                .update();
    }

    void remove(ActivityType type, long entryId) {
        jdbc.sql("DELETE FROM activities WHERE entry_id = :entryId AND type = :type")
                .param("entryId", entryId)
                .param("type", type.name())
                .update();
    }

    /** Das mais recentes para as mais antigas. */
    Page<ActivityDTO> feed(long userId, Pageable pageable) {
        long total = jdbc.sql("SELECT count(*)" + FEED)
                .param("userId", userId)
                .query(Long.class)
                .single();
        List<ActivityDTO> content = jdbc.sql("""
                        SELECT a.id, a.type, a.created_at, coalesce(a.data ->> 'status', e.status) AS status,
                               (a.data ->> 'completed')::boolean AS completed,
                               u.username, u.display_name,
                               g.id AS game_id, g.slug, g.title, g.cover_image_id, g.release_date,
                               e.rating, e.recommends, e.review_text, e.has_spoilers
                        """ + FEED + """
                         JOIN games g ON g.id = a.game_id
                         JOIN library_entries e ON e.id = a.entry_id
                        ORDER BY a.created_at DESC, a.id DESC
                        LIMIT :limit OFFSET :offset
                        """)
                .param("userId", userId)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .query((rs, row) -> {
                    ActivityType type = ActivityType.valueOf(rs.getString("type"));
                    return new ActivityDTO(
                            rs.getLong("id"),
                            type,
                            new ActivityDTO.Author(rs.getString("username"), rs.getString("display_name")),
                            GameSummaryDTO.of(
                                    rs.getLong("game_id"),
                                    rs.getString("slug"),
                                    rs.getString("title"),
                                    rs.getString("cover_image_id"),
                                    rs.getObject("release_date", LocalDate.class)),
                            EntryStatus.valueOf(rs.getString("status")),
                            type == ActivityType.STATUS ? rs.getBoolean("completed") : null,
                            type == ActivityType.REVIEW
                                    ? new ReviewDTO(
                                            rs.getBigDecimal("rating"),
                                            rs.getObject("recommends", Boolean.class),
                                            rs.getString("review_text"),
                                            rs.getObject("has_spoilers", Boolean.class))
                                    : null,
                            rs.getObject("created_at", OffsetDateTime.class).toInstant());
                })
                .list();
        return new PageImpl<>(content, pageable, total);
    }
}
