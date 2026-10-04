package com.kauan.gamelog.social;

import com.kauan.gamelog.shared.NotFoundException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Curtir e denunciar só valem para avaliações que aparecem nas listas: com texto e de perfil público. */
@Repository
class ListedReviews {
    private final JdbcClient jdbc;

    ListedReviews(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** @throws NotFoundException se a entrada não é uma avaliação listada */
    long authorOf(long entryId) {
        return jdbc.sql("""
                        SELECT e.user_id FROM library_entries e JOIN users u ON u.id = e.user_id
                        WHERE e.id = :entryId AND e.review_text IS NOT NULL AND u.profile_visibility = 'PUBLIC'
                        """)
                .param("entryId", entryId)
                .query(Long.class)
                .optional()
                .orElseThrow(() -> new NotFoundException("Avaliação não encontrada."));
    }
}
