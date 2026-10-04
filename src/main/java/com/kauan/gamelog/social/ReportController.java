package com.kauan.gamelog.social;

import com.kauan.gamelog.shared.security.CurrentUserId;
import com.kauan.gamelog.shared.security.OpenApiConfig;
import com.kauan.gamelog.social.dto.ReportRequest;
import com.kauan.gamelog.social.dto.ReportedReviewDTO;
import com.kauan.gamelog.social.dto.ResolveReportRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Denunciar exige login; moderar, o papel ADMIN (ver {@code SecurityConfig}). */
@RestController
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** Uma denúncia aberta por pessoa e avaliação; nunca da própria. */
    @PostMapping("/reviews/{entryId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public void report(
            @CurrentUserId Long userId, @PathVariable long entryId, @Valid @RequestBody ReportRequest request) {
        reportService.report(userId, entryId, request);
    }

    /** Avaliações com denúncias abertas: as mais denunciadas primeiro e, entre elas, as mais antigas. */
    @GetMapping("/admin/reports")
    public PagedModel<ReportedReviewDTO> open(Pageable pageable) {
        return new PagedModel<>(reportService.open(pageable));
    }

    /** Manter a avaliação ou tirar o texto; vale para todas as denúncias abertas dela. */
    @PatchMapping("/admin/reports/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolve(
            @CurrentUserId Long adminId, @PathVariable long id, @Valid @RequestBody ResolveReportRequest request) {
        reportService.resolve(adminId, id, request.decision());
    }
}
