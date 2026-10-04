package com.kauan.gamelog.social.dto;

import com.kauan.gamelog.social.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotNull(message = "escolha um motivo") ReportReason reason,

        @Size(max = 500, message = "use no máximo 500 caracteres")
        String details) {

    public ReportRequest {
        details = details == null || details.isBlank() ? null : details.strip();
    }
}
