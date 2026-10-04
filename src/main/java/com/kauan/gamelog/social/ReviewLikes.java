package com.kauan.gamelog.social;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** A tabela {@code review_likes}: quem curtiu qual avaliação. Curtir de novo não muda nada. */
@Repository
class ReviewLikes {
    private final JdbcClient jdbc;

    ReviewLikes(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** O autor, se a entrada é uma avaliação que aparece nas listas: tem texto e o perfil é público. */
    Optional<Long> authorOfPublicReview(long entryId) {
        return jdbc.sql("""
                        SELECT e.user_id FROM library_entries e JOIN users u ON u.id = e.user_id
                        WHERE e.id = :entryId AND e.review_text IS NOT NULL AND u.profile_visibility = 'PUBLIC'
                        """).param("entryId", entryId).query(Long.class).optional();
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

    List<Long> likedAmong(long userId, Collection<Long> entryIds) {
        return jdbc.sql("SELECT entry_id FROM review_likes WHERE user_id = :userId AND entry_id IN (:entryIds)")
                .param("userId", userId)
                .param("entryIds", entryIds)
                .query(Long.class)
                .list();
    }
}
