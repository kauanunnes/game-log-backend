package com.kauan.gamelog.catalog.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ImportGameRequest(
        @NotNull(message = "informe o id do IGDB") @Positive Long igdbId) {}
