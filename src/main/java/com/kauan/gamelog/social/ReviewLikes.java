package com.kauan.gamelog.social;

import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.social.dto.LikedReviewDTO;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** A tabela {@code review_likes}: quem curtiu qual avaliação. Curtir de novo não muda nada. */
@Repository
class ReviewLikes {
    private final JdbcClient jdbc;

    ReviewLikes(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    void add(long userId, long entryId) {
        jdbc.sql("INSERT INTO review_likes (user_id, entry_id) VALUES (:userId, :entryId) ON CONFLICT DO NOTHING")
                .param("userId", userId)
                .param("entryId", entryId)
                .update();
    }

    void remove(long userId, long entryId) {
        jdbc.sql("DELETE FROM review_likes WHERE user_id = :userId AND entry_id = :entryId")
                .param("userId", userId)
                .param("entryId", entryId)
                .update();
    }

    void removeAll(long entryId) {
        jdbc.sql("DELETE FROM review_likes WHERE entry_id = :entryId")
                .param("entryId", entryId)
                .update();
    }

    /** Das curtidas mais recentes para as mais antigas. */
    List<LikedReviewDTO> likedBy(long userId) {
        return jdbc.sql("""
                        SELECT l.entry_id, l.created_at, u.username,
                               g.id AS game_id, g.slug, g.title, g.cover_image_id, g.release_date
                        FROM review_likes l
                        JOIN library_entries e ON e.id = l.entry_id
                        JOIN users u ON u.id = e.user_id
                        JOIN games g ON g.id = e.game_id
                        WHERE l.user_id = :userId
                        ORDER BY l.created_at DESC
                        """)
                .param("userId", userId)
                .query((rs, row) -> new LikedReviewDTO(
                        rs.getLong("entry_id"),
                        rs.getString("username"),
                        GameSummaryDTO.of(
                                rs.getLong("game_id"),
                                rs.getString("slug"),
                                rs.getString("title"),
                                rs.getString("cover_image_id"),
                                rs.getObject("release_date", LocalDate.class)),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant()))
                .list();
    }

    List<Long> likedAmong(long userId, Collection<Long> entryIds) {
        return jdbc.sql("SELECT entry_id FROM review_likes WHERE user_id = :userId AND entry_id IN (:entryIds)")
                .param("userId", userId)
                .param("entryIds", entryIds)
                .query(Long.class)
                .list();
    }
}
