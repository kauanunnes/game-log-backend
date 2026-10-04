package com.kauan.gamelog.recommendation.dto;

import com.kauan.gamelog.catalog.dto.GameSummaryDTO;

/** Uma sugestão com o motivo, que cita um jogo da biblioteca da pessoa (RF62). */
public record SuggestionDTO(GameSummaryDTO game, String reason) {}
