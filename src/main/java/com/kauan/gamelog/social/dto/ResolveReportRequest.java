package com.kauan.gamelog.social.dto;

import com.kauan.gamelog.social.ReportDecision;
import jakarta.validation.constraints.NotNull;

public record ResolveReportRequest(
        @NotNull(message = "escolha manter ou remover") ReportDecision decision) {}
