package com.kauan.gamelog.social;

import com.kauan.gamelog.social.dto.FollowCounts;
import com.kauan.gamelog.social.dto.FollowDTO;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** A tabela {@code follows}. Uma linha por par, então seguir de novo não muda nada, nem a data. */
@Repository
class Follows {
    private final JdbcClient jdbc;

    Follows(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Os dois lados de uma lista: a coluna da dona da lista e a coluna de quem aparece nela. */
    enum Side {
        FOLLOWERS("followee_id", "follower_id"),
        FOLLOWING("follower_id", "followee_id");

        private final String owner;
        private final String listed;

        Side(String owner, String listed) {
            this.owner = owner;
            this.listed = listed;
        }
    }

    void add(long followerId, long followeeId) {
        jdbc.sql("""
                        INSERT INTO follows (follower_id, followee_id) VALUES (:followerId, :followeeId)
                        ON CONFLICT DO NOTHING
                        """)
                .param("followerId", followerId)
                .param("followeeId", followeeId)
                .update();
    }

    void remove(long followerId, long followeeId) {
        jdbc.sql("DELETE FROM follows WHERE follower_id = :followerId AND followee_id = :followeeId")
                .param("followerId", followerId)
                .param("followeeId", followeeId)
                .update();
    }

    boolean exists(long followerId, long followeeId) {
        return jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM follows WHERE follower_id = :followerId AND followee_id = :followeeId)
                        """)
                .param("followerId", followerId)
                .param("followeeId", followeeId)
                .query(Boolean.class)
                .single();
    }

    FollowCounts counts(long userId) {
        return jdbc.sql("""
                        SELECT (SELECT count(*) FROM follows WHERE followee_id = :userId) AS followers,
                               (SELECT count(*) FROM follows WHERE follower_id = :userId) AS following
                        """)
                .param("userId", userId)
                .query((rs, row) -> new FollowCounts(rs.getLong("followers"), rs.getLong("following")))
                .single();
    }

    /** Dos mais recentes para os mais antigos. */
    Page<FollowDTO> list(long userId, Side side, Pageable pageable) {
        long total = jdbc.sql("SELECT count(*) FROM follows WHERE " + side.owner + " = :userId")
                .param("userId", userId)
                .query(Long.class)
                .single();
        List<FollowDTO> content = jdbc.sql("""
                        SELECT u.username, u.display_name, f.created_at
                        FROM follows f JOIN users u ON u.id = f.%s
                        WHERE f.%s = :userId
                        ORDER BY f.created_at DESC, u.username
                        LIMIT :limit OFFSET :offset
                        """.formatted(side.listed, side.owner))
                .param("userId", userId)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .query((rs, row) -> new FollowDTO(
                        rs.getString("username"),
                        rs.getString("display_name"),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant()))
                .list();
        return new PageImpl<>(content, pageable, total);
    }
}
