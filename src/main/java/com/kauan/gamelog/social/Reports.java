package com.kauan.gamelog.social;

import com.kauan.gamelog.social.dto.ReportedReviewDTO.Report;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** A tabela {@code reports}. */
@Repository
class Reports {
    private final JdbcClient jdbc;

    Reports(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    record Found(long entryId, boolean open) {}

    /** @return {@code false} se a pessoa já tem uma denúncia aberta para essa avaliação */
    boolean add(long reporterId, long entryId, ReportReason reason, String details) {
        return jdbc.sql("""
                        INSERT INTO reports (reporter_id, entry_id, reason, details)
                        VALUES (:reporterId, :entryId, :reason, :details)
                        ON CONFLICT (reporter_id, entry_id) WHERE status = 'OPEN' DO NOTHING
                        """)
                        .param("reporterId", reporterId)
                        .param("entryId", entryId)
                        .param("reason", reason.name())
                        .param("details", details)
                        .update()
                == 1;
    }

    Optional<Found> find(long reportId) {
        return jdbc.sql("SELECT entry_id, status FROM reports WHERE id = :id")
                .param("id", reportId)
                .query((rs, row) ->
                        new Found(rs.getLong("entry_id"), rs.getString("status").equals("OPEN")))
                .optional();
    }

    /** Fecha todas as denúncias abertas da avaliação. */
    void resolve(long entryId, String status, Long adminId) {
        jdbc.sql("""
                        UPDATE reports SET status = :status, resolved_by = :adminId, resolved_at = now()
                        WHERE entry_id = :entryId AND status = 'OPEN'
                        """)
                .param("status", status)
                .param("adminId", adminId)
                .param("entryId", entryId)
                .update();
    }

    /** As avaliações com denúncias abertas: as mais denunciadas primeiro e, entre elas, as mais antigas. */
    Page<Long> openEntries(Pageable pageable) {
        long total = jdbc.sql("SELECT count(DISTINCT entry_id) FROM reports WHERE status = 'OPEN'")
                .query(Long.class)
                .single();
        List<Long> entryIds = jdbc.sql("""
                        SELECT entry_id FROM reports WHERE status = 'OPEN'
                        GROUP BY entry_id ORDER BY count(*) DESC, min(created_at), entry_id
                        LIMIT :limit OFFSET :offset
                        """)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .query(Long.class)
                .list();
        return new PageImpl<>(entryIds, pageable, total);
    }

    record OpenReport(long entryId, Report report) {}

    List<OpenReport> openOf(Collection<Long> entryIds) {
        if (entryIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("""
                        SELECT r.id, r.entry_id, r.reason, r.details, r.created_at, u.username
                        FROM reports r JOIN users u ON u.id = r.reporter_id
                        WHERE r.status = 'OPEN' AND r.entry_id IN (:entryIds)
                        ORDER BY r.created_at, r.id
                        """)
                .param("entryIds", entryIds)
                .query((rs, row) -> new OpenReport(
                        rs.getLong("entry_id"),
                        new Report(
                                rs.getLong("id"),
                                ReportReason.valueOf(rs.getString("reason")),
                                rs.getString("details"),
                                rs.getString("username"),
                                rs.getObject("created_at", OffsetDateTime.class).toInstant())))
                .list();
    }
}
