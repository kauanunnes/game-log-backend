package com.kauan.gamelog.social;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.kauan.gamelog.library.LibraryEntryChanged;
import com.kauan.gamelog.library.LibraryService;
import com.kauan.gamelog.library.dto.PublicReviewDTO;
import com.kauan.gamelog.shared.ConflictException;
import com.kauan.gamelog.shared.NotFoundException;
import com.kauan.gamelog.shared.UnprocessableException;
import com.kauan.gamelog.social.Reports.OpenReport;
import com.kauan.gamelog.social.dto.ReportRequest;
import com.kauan.gamelog.social.dto.ReportedReviewDTO;
import com.kauan.gamelog.social.dto.ReportedReviewDTO.Report;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/** Denunciar avaliações e moderar as denúncias (RF53, RN18). */
@Service
public class ReportService {
    private final Reports reports;
    private final ListedReviews listed;
    private final LibraryService library;

    ReportService(Reports reports, ListedReviews listed, LibraryService library) {
        this.reports = reports;
        this.listed = listed;
        this.library = library;
    }

    @Transactional
    public void report(long userId, long entryId, ReportRequest request) {
        if (listed.authorOf(entryId) == userId) {
            throw new UnprocessableException(
                    "CANNOT_REPORT_OWN_REVIEW", "Não dá para denunciar a própria avaliação.", List.of());
        }
        if (!reports.add(userId, entryId, request.reason(), request.details())) {
            throw new ConflictException("Você já denunciou esta avaliação, e a denúncia ainda está aberta.");
        }
    }

    @Transactional(readOnly = true)
    public Page<ReportedReviewDTO> open(Pageable pageable) {
        Page<Long> entryIds = reports.openEntries(pageable);
        Map<Long, PublicReviewDTO> reviews = library.reviewsByIds(entryIds.getContent()).stream()
                .collect(Collectors.toMap(PublicReviewDTO::id, Function.identity()));
        Map<Long, List<Report>> byEntry = reports.openOf(entryIds.getContent()).stream()
                .collect(groupingBy(OpenReport::entryId, mapping(OpenReport::report, toList())));
        return entryIds.map(id -> new ReportedReviewDTO(reviews.get(id), byEntry.get(id)));
    }

    /** Resolve de uma vez todas as denúncias abertas da avaliação. */
    @Transactional
    public void resolve(long adminId, long reportId, ReportDecision decision) {
        Reports.Found report =
                reports.find(reportId).orElseThrow(() -> new NotFoundException("Denúncia não encontrada."));
        if (!report.open()) {
            throw new ConflictException("Esta denúncia já foi resolvida.");
        }
        // Fecha antes de tirar o texto, para o ouvinte abaixo não ter o que fechar.
        reports.resolve(report.entryId(), decision == ReportDecision.REMOVE ? "REMOVED" : "KEPT", adminId);
        if (decision == ReportDecision.REMOVE) {
            library.removeReviewText(report.entryId());
        }
    }

    /** Se a avaliação perde o texto por outro caminho (o próprio autor), as denúncias abertas fecham sozinhas. */
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(LibraryEntryChanged event) {
        if (event.before() != null
                && event.after() != null
                && event.before().hasText()
                && !event.after().hasText()) {
            reports.resolve(event.entryId(), "REMOVED", null);
        }
    }
}
