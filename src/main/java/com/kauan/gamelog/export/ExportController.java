package com.kauan.gamelog.export;

import com.kauan.gamelog.export.dto.ExportDTO;
import com.kauan.gamelog.shared.security.CurrentUserId;
import com.kauan.gamelog.shared.security.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class ExportController {
    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /** Um JSON para baixar, com tudo o que a conta guardou. */
    @GetMapping("/me/export")
    public ResponseEntity<ExportDTO> export(@CurrentUserId Long userId) {
        ExportDTO data = exportService.export(userId);
        String file = "game-log-%s-%s.json"
                .formatted(data.account().username(), LocalDate.ofInstant(data.exportedAt(), ZoneOffset.UTC));
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file).build().toString())
                .body(data);
    }
}
